package br.com.rodorush.chartpatterntracker.data

import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.api.BrapiApiService
import br.com.rodorush.chartpatterntracker.api.BrapiResponse
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
            service.getHistoricalData(ticker, range, interval, apiKey).toCandles()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun requiresApiKey(): Boolean = true

    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
    }
}

/**
 * Velas da resposta da cotação da Brapi, com o horário em ms epoch.
 *
 * Só o `historicalDataPrice` presente e vazio vale como série sem velas, que é como a Brapi
 * responde para o ativo sem negócios no período. A resposta sem `results` ou sem o campo não diz
 * se há velas e vira falha: a varredura não pode avisá-la como série sem velas.
 */
internal fun BrapiResponse.toCandles(): Result<List<Candlestick>> {
    val history = results?.firstOrNull()?.historicalDataPrice
        ?: return Result.failure(IllegalStateException("Resposta da Brapi sem historicalDataPrice"))
    return Result.success(
        history.map { historical ->
            Candlestick(
                time = historical.date * 1000L, // Converte segundos para milissegundos
                open = historical.open.toFloat(),
                high = historical.high.toFloat(),
                low = historical.low.toFloat(),
                close = historical.close.toFloat(),
                volume = historical.volume
            )
        }
    )
}
