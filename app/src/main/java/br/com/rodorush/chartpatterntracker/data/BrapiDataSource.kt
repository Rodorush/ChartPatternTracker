package br.com.rodorush.chartpatterntracker.data

import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.api.BrapiApiService
import br.com.rodorush.chartpatterntracker.model.Candlestick
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

    override suspend fun getHistoricalData(ticker: String, range: String, interval: String): Result<List<Candlestick>> {
        return try {
            val response = service.getHistoricalData(ticker, range, interval, apiKey)
            val data = response.results.firstOrNull()?.historicalDataPrice?.map { historical ->
                Candlestick(
                    time = historical.date * 1000L, // Converte segundos para milissegundos
                    open = historical.open.toFloat(),
                    high = historical.high.toFloat(),
                    low = historical.low.toFloat(),
                    close = historical.close.toFloat(),
                    volume = historical.volume
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