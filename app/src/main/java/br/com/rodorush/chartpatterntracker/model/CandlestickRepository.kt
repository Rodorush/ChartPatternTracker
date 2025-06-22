package br.com.rodorush.chartpatterntracker.model

import android.util.Log
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import br.com.rodorush.chartpatterntracker.BuildConfig

class CandlestickRepository(
    private val dao: CandlestickDao,
    private val firestoreService: FirestoreService
) {
    suspend fun fetchCandlesticks(ticker: String, timeframe: String, range: String = "3mo"): List<Candlestick> {
        return withContext(Dispatchers.IO) {
            Log.d("CandlestickRepository", "Iniciando fetchCandlesticks para ticker=$ticker, timeframe=$timeframe, range=$range")
            val today = System.currentTimeMillis()
            val threeMonthsAgo = today - (90L * 24 * 60 * 60 * 1000) // 90 dias em ms
            val latest = dao.getLatestTimestamp(ticker, timeframe) ?: 0
            Log.d("CandlestickRepository", "Latest timestamp para $ticker-$timeframe: $latest, threeMonthsAgo=$threeMonthsAgo, today=$today")

            try {
                // Obter candlesticks existentes no Room
                val existingCandlesticks = dao.getCandlesticks(ticker, timeframe, threeMonthsAgo).toCandlesticks()
                val existingTimes = existingCandlesticks.map { it.time }.toSet()
                Log.d("CandlestickRepository", "Candlesticks existentes no Room para $ticker-$timeframe: ${existingCandlesticks.size}")

                if (latest >= today - 24 * 60 * 60 * 1000 && latest >= threeMonthsAgo && existingCandlesticks.isNotEmpty()) {
                    Log.d("CandlestickRepository", "Dados locais recentes encontrados para $ticker-$timeframe: ${existingCandlesticks.size} candlesticks")
                    existingCandlesticks
                } else {
                    Log.d("CandlestickRepository", "Consultando Firestore para $ticker-$timeframe")
                    val remoteCandlesticks = firestoreService.fetchCandlesticks(ticker, timeframe, latest, today)
                    Log.d("CandlestickRepository", "Candlesticks remotos recebidos para $ticker-$timeframe: ${remoteCandlesticks.size}")
                    val candlesticksToSave = if (remoteCandlesticks.isEmpty() || remoteCandlesticks.last().time < today - 24 * 60 * 60 * 1000) {
                        Log.d("CandlestickRepository", "Chamando updateCandlesticks para $ticker-$timeframe")
                        val updatedCandlesticks = callUpdateCandlesticks(ticker, timeframe)
                        Log.d("CandlestickRepository", "Candlesticks atualizados recebidos: ${updatedCandlesticks.size}")
                        // Filtrar candlesticks não existentes no Room
                        (remoteCandlesticks + updatedCandlesticks).distinctBy { it.time }.filter { it.time !in existingTimes }
                    } else {
                        remoteCandlesticks.filter { it.time !in existingTimes }
                    }

                    if (candlesticksToSave.isNotEmpty()) {
                        Log.d("CandlestickRepository", "Salvando ${candlesticksToSave.size} novos candlesticks no Room para $ticker-$timeframe")
                        dao.insertAll(candlesticksToSave.toEntities(ticker, timeframe))
                        // Forçar sincronização do Room
                        dao.getCandlesticks(ticker, timeframe, 0) // Consulta dummy
                    } else {
                        Log.d("CandlestickRepository", "Nenhum novo candlestick para salvar no Room para $ticker-$timeframe")
                    }

                    // Retornar todos os candlesticks do Room
                    val finalCandlesticks = dao.getCandlesticks(ticker, timeframe, threeMonthsAgo).toCandlesticks()
                    Log.d("CandlestickRepository", "Retornando ${finalCandlesticks.size} candlesticks para $ticker-$timeframe")
                    finalCandlesticks
                }
            } catch (e: Exception) {
                Log.e("CandlestickRepository", "Erro em fetchCandlesticks para $ticker-$timeframe: ${e.message}", e)
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
        Log.d("CandlestickRepository", "Parâmetros enviados para updateCandlesticks: ${params.entries.joinToString()}")
        try {
            val result = callable.call(params).await()
            Log.d("CandlestickRepository", "Resposta de updateCandlesticks: ${result.data}")
            @Suppress("UNCHECKED_CAST")
            val data = result.data as List<Map<String, Any>>
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
                    Log.e("CandlestickRepository", "Erro ao parsear candlestick: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("CandlestickRepository", "Erro ao chamar updateCandlesticks: ${e.message}", e)
            throw e
        }
    }

    suspend fun clearOldData(ticker: String, timeframe: String, thresholdDays: Long = 180) {
        val threshold = System.currentTimeMillis() - thresholdDays * 24 * 60 * 60 * 1000
        Log.d("CandlestickRepository", "Limpando dados antigos para $ticker-$timeframe anteriores a $threshold")
        dao.deleteOld(ticker, timeframe, threshold)
    }

    suspend fun detectHaramiAlta(candlesticks: List<Candlestick>): List<Candlestick> {
        return withContext(Dispatchers.Default) {
            Log.d("CandlestickRepository", "Detectando Harami de Alta em ${candlesticks.size} candlesticks")
            val haramiCandles = mutableListOf<Candlestick>()
            if (candlesticks.size < 2) return@withContext haramiCandles

            for (i in 1 until candlesticks.size) {
                val current = candlesticks[i]
                val previous = candlesticks[i - 1]

                // Critérios do Harami de Alta
                val isBullish = current.close > current.open
                val isBearish = previous.open > previous.close
                val isContained = current.open > previous.close && current.close < previous.open

                if (isBullish && isBearish && isContained) {
                    // Marca apenas a primeira vela do padrão
                    haramiCandles.add(previous)
                }
            }
            Log.d("CandlestickRepository", "Padrões Harami de Alta detectados: ${haramiCandles.size}")
            haramiCandles.distinct()
        }
    }
}