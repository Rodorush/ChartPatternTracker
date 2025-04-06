package br.com.rodorush.chartpatterntracker.data

import br.com.rodorush.chartpatterntracker.model.Candlestick

interface AssetDataSource {
    suspend fun getHistoricalData(
        ticker: String,
        range: String,
        interval: String
    ): Result<List<Candlestick>>

    fun requiresApiKey(): Boolean
    fun setApiKey(apiKey: String)
}