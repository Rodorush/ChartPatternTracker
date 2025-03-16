package br.com.rodorush.chartpatterntracker.data

import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.Time

class MockDataSource : AssetDataSource {
    override suspend fun getHistoricalData(ticker: String, range: String, interval: String): Result<List<CandlestickData>> {
        val mockData = listOf(
            CandlestickData(Time.Utc(1741698000), 10.49f, 10.51f, 10.34f, 10.40f),
            CandlestickData(Time.Utc(1741784400), 10.40f, 10.52f, 10.35f, 10.50f),
            CandlestickData(Time.Utc(1741870800), 10.98f, 11.63f, 10.93f, 11.60f),
            CandlestickData(Time.Utc(1741957200), 11.73f, 12.90f, 11.70f, 12.87f)
        )
        return Result.success(mockData)
    }

    override fun requiresApiKey(): Boolean = false

    override fun setApiKey(apiKey: String) {
        // Não faz nada, pois mock não precisa de chave
    }
}