package br.com.rodorush.chartpatterntracker.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface BrapiApiService {
    @GET("api/quote/list")
    suspend fun getQuoteList(@Header("Authorization") authorization: String): QuoteListResponse

    @GET("api/quote/{ticker}")
    suspend fun getHistoricalData(
        @Path("ticker") ticker: String,
        @Query("range") range: String = "3mo", // Adicionando range como opcional
        @Query("interval") interval: String,
        @Query("token") token: String
    ): BrapiResponse

    data class QuoteListResponse(
        val stocks: List<StockQuote>
    )

    data class StockQuote(
        val stock: String,
        val name: String,
        val close: Double?,
        val change: Double?
    )
}