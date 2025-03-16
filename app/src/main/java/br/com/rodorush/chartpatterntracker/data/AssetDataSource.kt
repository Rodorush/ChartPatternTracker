package br.com.rodorush.chartpatterntracker.data

import com.tradingview.lightweightcharts.api.series.models.CandlestickData

interface AssetDataSource {
    suspend fun getHistoricalData(
        ticker: String,
        range: String,
        interval: String
    ): Result<List<CandlestickData>>

    fun requiresApiKey(): Boolean
    fun setApiKey(apiKey: String)
}