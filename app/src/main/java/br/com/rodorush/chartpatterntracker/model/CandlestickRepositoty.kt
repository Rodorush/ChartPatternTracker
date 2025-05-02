package br.com.rodorush.chartpatterntracker.model

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
            val today = System.currentTimeMillis()
            val threeMonthsAgo = today - 90L * 24 * 60 * 60 * 1000
            val latest = dao.getLatestTimestamp(ticker, timeframe) ?: 0

            if (latest >= today - 24 * 60 * 60 * 1000 && latest >= threeMonthsAgo) {
                dao.getCandlesticks(ticker, timeframe, threeMonthsAgo).toCandlesticks()
            } else {
                val remoteCandlesticks = firestoreService.fetchCandlesticks(ticker, timeframe, latest, today)
                if (remoteCandlesticks.isEmpty() || remoteCandlesticks.last().time < today - 24 * 60 * 60 * 1000) {
                    val startDate = if (latest == 0L) {
                        firestoreService.timestampToDateString(threeMonthsAgo)
                    } else {
                        firestoreService.timestampToDateString(latest)
                    }
                    val endDate = firestoreService.timestampToDateString(today)
                    val updatedCandlesticks = callUpdateCandlesticks(ticker, timeframe, startDate, endDate)
                    firestoreService.saveCandlesticks(ticker, timeframe, updatedCandlesticks)
                    remoteCandlesticks + updatedCandlesticks
                } else {
                    remoteCandlesticks
                }.also { candlesticks ->
                    dao.insertAll(candlesticks.toEntities(ticker, timeframe))
                }
            }
        }
    }

    private suspend fun callUpdateCandlesticks(
        ticker: String,
        timeframe: String,
        startDate: String,
        endDate: String
    ): List<Candlestick> {
        val callable = Firebase.functions.getHttpsCallable("updateCandlesticks")
        val result = callable.call(
            mapOf(
                "ticker" to ticker,
                "timeframe" to timeframe,
                "startDate" to startDate,
                "endDate" to endDate
            )
        ).await()

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
                null
            }
        }
    }

    suspend fun clearOldData(ticker: String, timeframe: String, thresholdDays: Long = 180) {
        val threshold = System.currentTimeMillis() - thresholdDays * 24 * 60 * 60 * 1000
        dao.deleteOld(ticker, timeframe, threshold)
    }

    suspend fun detectHaramiAlta(candlesticks: List<Candlestick>): List<Candlestick> {
        return withContext(Dispatchers.Default) {
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
            haramiCandles.distinct() // Remove duplicatas se houver
        }
    }
}