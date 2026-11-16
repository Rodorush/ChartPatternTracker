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

    suspend fun fetchCandlesticks(ticker: String, timeframe: String, range: String = "3mo"): List<Candlestick> {
        return withContext(Dispatchers.IO) {
            val today = System.currentTimeMillis()
            val threeMonthsAgo = today - NINETY_DAYS_MS
            val latest = dao.getLatestTimestamp(ticker, timeframe) ?: 0

            try {
                val existingCandlesticks = dao.getCandlesticks(ticker, timeframe, threeMonthsAgo).toCandlesticks()
                val existingTimes = existingCandlesticks.map { it.time }.toSet()

                if (CandleFreshness.isUpToDate(latest, today, timeframe) && latest >= threeMonthsAgo && existingCandlesticks.isNotEmpty()) {
                    Log.d(TAG, "fetchCandlesticks($ticker,$timeframe): cache local com ${existingCandlesticks.size} velas")
                    existingCandlesticks
                } else {
                    val remoteCandlesticks = firestoreService.fetchCandlesticks(ticker, timeframe, latest, today)
                    val candlesticksToSave = if (remoteCandlesticks.isEmpty() || !CandleFreshness.isUpToDate(remoteCandlesticks.last().time, today, timeframe)) {
                        val updatedCandlesticks = refusedPairs.guard(ticker, timeframe) {
                            callUpdateCandlesticks(ticker, timeframe)
                        }
                        (remoteCandlesticks + updatedCandlesticks).distinctBy { it.time }.filter { it.time !in existingTimes }
                    } else {
                        remoteCandlesticks.filter { it.time !in existingTimes }
                    }

                    if (candlesticksToSave.isNotEmpty()) {
                        dao.insertAll(candlesticksToSave.toEntities(ticker, timeframe))
                    }

                    val finalCandlesticks = dao.getCandlesticks(ticker, timeframe, threeMonthsAgo).toCandlesticks()
                    Log.d(TAG, "fetchCandlesticks($ticker,$timeframe): ${finalCandlesticks.size} velas (remoto=${remoteCandlesticks.size}, novas=${candlesticksToSave.size})")
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

    private suspend fun callUpdateCandlesticks(
        ticker: String,
        timeframe: String
    ): List<Candlestick> {
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
            return data.mapNotNull { map ->
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