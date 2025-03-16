package br.com.rodorush.chartpatterntracker.data

import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.api.BrapiApiService
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.Time
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BrapiDataSource : AssetDataSource {
    private var apiKey: String = BuildConfig.BRAPI_TOKEN
    private val service: BrapiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://brapi.dev/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BrapiApiService::class.java)
    }

    override suspend fun getHistoricalData(ticker: String, range: String, interval: String): Result<List<CandlestickData>> {
        return try {
            val response = service.getHistoricalData(ticker, range, interval, apiKey)
            val data = response.results.firstOrNull()?.historicalDataPrice?.map { historical ->
                CandlestickData(
                    time = Time.Utc(historical.date / 1000),
                    open = historical.open.toFloat(),
                    high = historical.high.toFloat(),
                    low = historical.low.toFloat(),
                    close = historical.close.toFloat()
                )
            } ?: emptyList()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun requiresApiKey(): Boolean = true

    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
    }
}