package br.com.rodorush.chartpatterntracker.model

import br.com.rodorush.customlogs.Log
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import br.com.rodorush.chartpatterntracker.model.pattern.PatternDetectorRegistry

class CandlestickRepository(
    private val dao: CandlestickDao,
    private val firestoreService: FirestoreService
) {
    private val refusedPairs = RefusedPairs()

    /**
     * Velas dos últimos 90 dias do par, pelo cache em camadas: o Room, depois o Firestore e,
     * por fim, a `updateCandlesticks`, que consulta a Brapi. Cada camada só serve enquanto
     * [CandleFreshness.isUpToDate] a der como atualizada.
     *
     * O que chega do Firestore ou da função regrava no Room a vela de mesmo horário: a última
     * vela da série pode ter sido lida em formação, e a versão fechada tem de substituí-la.
     * Cada vela vai para o Room com o instante em que a função a leu da Brapi.
     *
     * @param range não é usado: a função busca sempre 3 meses.
     * @throws IntervalNotInPlanException quando o plano do token da Brapi recusa o par.
     */
    suspend fun fetchCandlesticks(ticker: String, timeframe: String, range: String = "3mo"): List<Candlestick> {
        return withContext(Dispatchers.IO) {
            val today = System.currentTimeMillis()
            val threeMonthsAgo = today - NINETY_DAYS_MS
            val latest = dao.getLatestTimestamp(ticker, timeframe) ?: 0
            val lastFetch = dao.getLastFetchTime(ticker, timeframe) ?: 0

            try {
                val existingCandlesticks = dao.getCandlesticks(ticker, timeframe, threeMonthsAgo).toCandlesticks()

                if (CandleFreshness.isUpToDate(latest, lastFetch, today, timeframe) && latest >= threeMonthsAgo && existingCandlesticks.isNotEmpty()) {
                    Log.d(TAG, "fetchCandlesticks($ticker,$timeframe): cache local com ${existingCandlesticks.size} velas")
                    existingCandlesticks
                } else {
                    val remote = firestoreService.fetchCandlesticks(ticker, timeframe, latest, today)
                    val remoteUpToDate = remote.candles.isNotEmpty() &&
                        CandleFreshness.isUpToDate(remote.candles.last().time, remote.fetchedAt, today, timeframe)
                    // Com o Firestore desatualizado, vale só a resposta da função: ela traz os
                    // 3 meses e é mais nova que a leitura do Firestore, feita antes da chamada.
                    val fetched = if (remoteUpToDate) {
                        remote
                    } else {
                        refusedPairs.guard(ticker, timeframe) {
                            callUpdateCandlesticks(ticker, timeframe, today)
                        }
                    }

                    if (fetched.candles.isNotEmpty()) {
                        dao.insertAll(fetched.candles.toEntities(ticker, timeframe, fetched.fetchedAt))
                    }

                    val finalCandlesticks = dao.getCandlesticks(ticker, timeframe, threeMonthsAgo).toCandlesticks()
                    val source = if (remoteUpToDate) "Firestore" else "função"
                    Log.d(TAG, "fetchCandlesticks($ticker,$timeframe): ${finalCandlesticks.size} velas (origem=$source, gravadas=${fetched.candles.size})")
                    finalCandlesticks
                }
            } catch (e: IntervalNotInPlanException) {
                // Recusa esperada: a primeira do par vira breadcrumb em callUpdateCandlesticks;
                // as seguintes vêm de RefusedPairs, sem chamar a função, só com Log.d.
                throw e
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Erro em fetchCandlesticks",
                    e,
                    "ticker" to ticker,
                    "timeframe" to timeframe
                )
                throw e
            }
        }
    }

    /**
     * Velas dos últimos 90 dias que o Room guarda para o par, sem conferir se a série ainda
     * vale: pode estar vencida. É a última tentativa quando nenhuma fonte responde, e quem a
     * usa avisa que os dados podem estar desatualizados. Lista vazia quando o par nunca foi lido.
     */
    suspend fun cachedCandlesticks(ticker: String, timeframe: String): List<Candlestick> {
        return withContext(Dispatchers.IO) {
            val threeMonthsAgo = System.currentTimeMillis() - NINETY_DAYS_MS
            dao.getCandlesticks(ticker, timeframe, threeMonthsAgo).toCandlesticks()
        }
    }

    /**
     * @param calledAt instante da chamada, em ms epoch. Vira o carimbo da leitura quando a
     * resposta não traz `fetchedAt`.
     */
    private suspend fun callUpdateCandlesticks(
        ticker: String,
        timeframe: String,
        calledAt: Long
    ): RemoteCandlesticks {
        val callable = Firebase.functions.getHttpsCallable("updateCandlesticks")
        val params = mapOf(
            "ticker" to ticker,
            "timeframe" to timeframe,
            "brapiToken" to BuildConfig.BRAPI_TOKEN
        )
        try {
            val result = callable.call(params).await()
            val data = (result.data as? List<*>)?.filterIsInstance<Map<String, Any>>().orEmpty()
            Log.d(TAG, "updateCandlesticks($ticker,$timeframe): ${data.size} velas recebidas")
            // O carimbo é o do servidor, o mesmo que a função grava no Firestore: assim o Room
            // e o Firestore medem a leitura pelo mesmo relógio.
            val fetchedAt = data.firstNotNullOfOrNull { (it["fetchedAt"] as? Number)?.toLong() } ?: calledAt
            val candles = data.mapNotNull { map ->
                try {
                    Candlestick(
                        time = (map["time"] as Number).toLong(),
                        open = (map["open"] as Number).toFloat(),
                        high = (map["high"] as Number).toFloat(),
                        low = (map["low"] as Number).toFloat(),
                        close = (map["close"] as Number).toFloat(),
                        volume = (map["volume"] as Number?)?.toLong()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao parsear candlestick: ${e.message}", e)
                    null
                }
            }
            return RemoteCandlesticks(candles, fetchedAt)
        } catch (e: Exception) {
            if (e is FirebaseFunctionsException && isIntervalNotInPlan(e.code.name, e.details)) {
                // Sem Throwable, fica só como breadcrumb e não vira não fatal no Crashlytics.
                // O par vai no texto, não em pares chave/valor: esses viram custom keys, que
                // ficariam presas ao próximo crash sem relação com a recusa.
                Log.w(TAG, "Intervalo fora do plano do token da Brapi: $ticker-$timeframe")
                throw IntervalNotInPlanException(ticker, timeframe, e)
            }
            Log.e(
                TAG,
                "Erro ao chamar updateCandlesticks",
                e,
                "ticker" to ticker,
                "timeframe" to timeframe
            )
            throw e
        }
    }

    suspend fun clearOldData(ticker: String, timeframe: String, thresholdDays: Long = 180) {
        val threshold = System.currentTimeMillis() - thresholdDays * ONE_DAY_MS
        dao.deleteOld(ticker, timeframe, threshold)
    }

    suspend fun detectPattern(patternId: String, candlesticks: List<Candlestick>): List<PatternOccurrence> {
        return withContext(Dispatchers.Default) {
            val detector = PatternDetectorRegistry.get(patternId)
            if (detector == null) {
                Log.w(TAG, "Nenhum detector encontrado para $patternId")
                emptyList()
            } else {
                detector.detect(candlesticks)
            }
        }
    }

    suspend fun detectPatterns(patternIds: List<String>, candlesticks: List<Candlestick>): Map<String, List<PatternOccurrence>> {
        val results = mutableMapOf<String, List<PatternOccurrence>>()
        for (id in patternIds) {
            results[id] = detectPattern(id, candlesticks)
        }
        return results
    }

    private companion object {
        const val TAG = "CandlestickRepository"
        const val ONE_DAY_MS = 24L * 60 * 60 * 1000
        const val NINETY_DAYS_MS = 90L * ONE_DAY_MS
    }
}