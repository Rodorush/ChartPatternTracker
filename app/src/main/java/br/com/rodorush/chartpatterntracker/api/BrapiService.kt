package br.com.rodorush.chartpatterntracker.api

import retrofit2.http.GET
import retrofit2.http.Query

interface BrapiService {
    @GET("api/available")
    suspend fun getAvailableAssets(@Query("token") token: String): AvailableAssetsResponse
}

data class AvailableAssetsResponse(
    val stocks: List<String>
)