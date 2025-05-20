package br.com.rodorush.chartpatterntracker.model

import android.util.Log
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CandlestickRepository(
    private val dao: CandlestickDao,
    private val firestoreService: FirestoreService
) {
    suspend fun fetchCandlesticks(ticker: String, timeframe: String, range: String = "3mo"): List<Candlestick> {
        return withContext(Dispatchers.IO) {
            Log.d("CandlestickRepository", "Iniciando fetchCandlesticks para ticker=$ticker, timeframe=$timeframe, range=$range")
            val today = System.currentTimeMillis()
            val threeMonthsAgo = today - 90L * 24 * 60 * 60 * 1000
            val latest = dao.getLatestTimestamp(ticker, timeframe) ?: 0

            if (latest >= today - 24 * 60 * 60 * 1000 && latest >= threeMonthsAgo) {
                val localCandlesticks = dao.getCandlesticks(ticker, timeframe, threeMonthsAgo).toCandlesticks()
                Log.d("CandlestickRepository", "Dados locais encontrados para $ticker-$timeframe: ${localCandlesticks.size} candlesticks")
                localCandlesticks
            } else {
                Log.d("CandlestickRepository", "Consultando Firestore para $ticker-$timeframe")
                val remoteCandlesticks = firestoreService.fetchCandlesticks(ticker, timeframe, latest, today)
                if (remoteCandlesticks.isEmpty() || remoteCandlesticks.last().time < today - 24 * 60 * 60 * 1000) {
                    Log.d("CandlestickRepository", "Chamando updateCandlesticks para $ticker-$timeframe")
                    val updatedCandlesticks = callUpdateCandlesticks(ticker, timeframe)
                    Log.d("CandlestickRepository", "Candlesticks atualizados recebidos: ${updatedCandlesticks.size}")
                    firestoreService.saveCandlesticks(ticker, timeframe, updatedCandlesticks)
                    (remoteCandlesticks + updatedCandlesticks).also { candlesticks ->
                        Log.d("CandlestickRepository", "Salvando ${candlesticks.size} candlesticks no Room para $ticker-$timeframe")
                        dao.insertAll(candlesticks.toEntities(ticker, timeframe))
                    }
                } else {
                    Log.d("CandlestickRepository", "Dados do Firestore encontrados para $ticker-$timeframe: ${remoteCandlesticks.size} candlesticks")
                    remoteCandlesticks.also { candlesticks ->
                        dao.insertAll(candlesticks.toEntities(ticker, timeframe))
                    }
                }
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
            "timeframe" to timeframe
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
                    haramiCandles.add(current)
                    haramiCandles.add(previous)
                }
            }
            Log.d("CandlestickRepository", "Padrões Harami de Alta detectados: ${haramiCandles.size}")
            haramiCandles.distinct()
        }
    }
}