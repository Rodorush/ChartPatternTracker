package br.com.rodorush.chartpatterntracker.data

import br.com.rodorush.chartpatterntracker.model.Candlestick
import kotlin.random.Random

class MockDataSource : AssetDataSource {
    override suspend fun getHistoricalData(ticker: String, range: String, interval: String): Result<List<Candlestick>> {
        // Ajustar o início para uma data mais recente, alinhada com Brapi
        val currentTime = System.currentTimeMillis()
        val millisecondsInDay = 86_400_000L
        val daysIn3Months = 90 // Aproximadamente 3 meses
        val numberOfDays = when (range) {
            "3mo" -> daysIn3Months
            else -> daysIn3Months // Padrão
        }

        val mockData = mutableListOf<Candlestick>()
        var lastClose = 37.3f // Valor inicial próximo ao PETR4 real

        for (day in 0 until numberOfDays) {
            val time = currentTime - (numberOfDays - day - 1) * millisecondsInDay
            val open = lastClose
            val variation = Random.nextFloat() * 2.0f - 1.0f // Variação maior para realismo
            val close = (open + variation).coerceIn(30f, 45f) // Intervalo realista
            val high = maxOf(open, close) + Random.nextFloat() * 0.5f
            val low = minOf(open, close) - Random.nextFloat() * 0.5f
            val volume = Random.nextLong(10_000_000, 50_000_000) // Volume realista

            mockData.add(
                Candlestick(
                    time = time,
                    open = open,
                    high = high.coerceAtMost(45f),
                    low = low.coerceAtLeast(30f),
                    close = close,
                    volume = volume
                )
            )
            lastClose = close
        }

        return Result.success(mockData)
    }

    override fun requiresApiKey(): Boolean = false

    override fun setApiKey(apiKey: String) {
        // No-op
    }
}