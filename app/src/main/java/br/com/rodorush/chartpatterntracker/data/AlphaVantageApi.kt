package br.com.rodorush.chartpatterntracker.data

import br.com.rodorush.chartpatterntracker.model.Candlestick
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Locale

interface AlphaVantageApiService {
    @GET("query")
    suspend fun getTimeSeries(
        @Query("function") function: String = "TIME_SERIES_DAILY",
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("apikey") apiKey: String
    ): AlphaVantageResponse
}

data class AlphaVantageResponse(
    val timeSeries: Map<String, TimeSeriesData>?
)

data class TimeSeriesData(
    val open: String,
    val high: String,
    val low: String,
    val close: String
)

class AlphaVantageDataSource : AssetDataSource {
    private var apiKey: String = ""
    private val service: AlphaVantageApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.alphavantage.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlphaVantageApiService::class.java)
    }

    override suspend fun getHistoricalData(ticker: String, range: String, interval: String): Result<List<Candlestick>> {
        return try {
            val response = service.getTimeSeries(symbol = ticker, interval = interval, apiKey = apiKey)
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val data = response.timeSeries?.map { (time, values) ->
                val timestamp = formatter.parse(time)?.time ?: 0L // Já em milissegundos
                Candlestick(
                    time = timestamp,
                    open = values.open.toFloat(),
                    high = values.high.toFloat(),
                    low = values.low.toFloat(),
                    close = values.close.toFloat()
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