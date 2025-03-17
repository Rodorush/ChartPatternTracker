package br.com.rodorush.chartpatterntracker.data

import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.Time
import kotlin.random.Random

class MockDataSource : AssetDataSource {
    override suspend fun getHistoricalData(ticker: String, range: String, interval: String): Result<List<CandlestickData>> {
        // Timestamp inicial (em segundos): 1741698000 (aproximadamente 2025-03-11)
        val initialTimestamp = 1741698000L
        val secondsInDay = 86_400L // 24 horas em segundos
        val numberOfDays = 104 // 4 dias originais + 100 dias adicionais

        // Gerar dados fictícios
        val mockData = mutableListOf<CandlestickData>()
        var lastClose = 10.40f // Valor de fechamento inicial baseado no primeiro candle original

        for (day in 0 until numberOfDays) {
            val time = Time.Utc(initialTimestamp + day * secondsInDay)

            // Simular variação nos preços
            val open = lastClose
            val variation = Random.nextFloat() * 0.5f - 0.25f // Variação aleatória entre -0.25 e +0.25
            val close = (open + variation).coerceIn(5f, 15f) // Limitar entre 5 e 15 para realismo
            val high = maxOf(open, close) + Random.nextFloat() * 0.2f // Máximo um pouco acima
            val low = minOf(open, close) - Random.nextFloat() * 0.2f // Mínimo um pouco abaixo

            mockData.add(
                CandlestickData(
                    time = time,
                    open = open,
                    high = high.coerceAtMost(15f), // Limite superior
                    low = low.coerceAtLeast(5f),  // Limite inferior
                    close = close
                )
            )
            lastClose = close // Atualizar o último fechamento para o próximo dia
        }

        return Result.success(mockData)
    }

    override fun requiresApiKey(): Boolean = false

    override fun setApiKey(apiKey: String) {
        // Não faz nada, pois mock não precisa de chave
    }
}