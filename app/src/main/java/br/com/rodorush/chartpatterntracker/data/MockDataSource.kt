package br.com.rodorush.chartpatterntracker.data

import br.com.rodorush.chartpatterntracker.model.Candlestick
import kotlin.random.Random

class MockDataSource : AssetDataSource {
    override suspend fun getHistoricalData(ticker: String, range: String, interval: String): Result<List<Candlestick>> {
        val initialTimestamp = 1741698000L * 1000L // Aproximadamente 2025-03-11 em milissegundos
        val millisecondsInDay = 86_400_000L // 24 horas em milissegundos
        val numberOfDays = 104 // 4 dias originais + 100 adicionais

        val mockData = mutableListOf<Candlestick>()
        var lastClose = 10.40f // Valor inicial de fechamento

        for (day in 0 until numberOfDays) {
            val time = initialTimestamp + day * millisecondsInDay
            val open = lastClose
            val variation = Random.nextFloat() * 0.5f - 0.25f // Variação aleatória entre -0.25 e +0.25
            val close = (open + variation).coerceIn(5f, 15f) // Limita entre 5 e 15
            val high = maxOf(open, close) + Random.nextFloat() * 0.2f
            val low = minOf(open, close) - Random.nextFloat() * 0.2f

            mockData.add(
                Candlestick(
                    time = time,
                    open = open,
                    high = high.coerceAtMost(15f),
                    low = low.coerceAtLeast(5f),
                    close = close
                )
            )
            lastClose = close
        }

        return Result.success(mockData)
    }

    override fun requiresApiKey(): Boolean = false

    override fun setApiKey(apiKey: String) {
        // No-op, mock não precisa de API key
    }
}