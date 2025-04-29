package br.com.rodorush.chartpatterntracker.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CandlestickRepository(private val dao: CandlestickDao) {
    suspend fun fetchCandlesticks(ticker: String, timeframe: String, range: String = "3mo"): List<Candlestick> {
        return withContext(Dispatchers.IO) {
            val today = System.currentTimeMillis()
            val threeMonthsAgo = today - 90L * 24 * 60 * 60 * 1000 // Aproximadamente 3 meses
            val latest = dao.getLatestTimestamp(ticker, timeframe) ?: 0

            if (latest >= today - 24 * 60 * 60 * 1000) {
                // Dados locais recentes
                dao.getCandlesticks(ticker, timeframe, threeMonthsAgo).toCandlesticks()
            } else {
                // TODO: Consultar Firestore (próximo passo)
                // Simulação com dados vazios por enquanto
                emptyList<Candlestick>().also { remote ->
                    dao.insertAll(remote.toEntities(ticker, timeframe))
                }
            }
        }
    }

    suspend fun clearOldData(ticker: String, timeframe: String, thresholdDays: Long = 180) {
        val threshold = System.currentTimeMillis() - thresholdDays * 24 * 60 * 60 * 1000
        dao.deleteOld(ticker, timeframe, threshold)
    }
}