package br.com.rodorush.chartpatterntracker.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BrapiApiService {
    @GET("api/available")
    suspend fun getAvailableAssets(@Query("token") token: String): AvailableAssetsResponse

    @GET("api/quote/{ticker}")
    suspend fun getHistoricalData(
        @Path("ticker") ticker: String,
        @Query("range") range: String = "3mo", // Adicionando range como opcional
        @Query("interval") interval: String,
        @Query("token") token: String
    ): BrapiResponse

    data class AvailableAssetsResponse(
        val stocks: List<String>
    )
}