package br.com.rodorush.chartpatterntracker.util.provider

import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.api.BrapiApiService
import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AssetsProvider
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BrapiAssetsProvider : AssetsProvider {
    private val token = BuildConfig.BRAPI_TOKEN
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://brapi.dev/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(BrapiApiService::class.java)

    override suspend fun fetchAssets(onResult: (List<AssetItem>) -> Unit) {
        try {
            val response = service.getQuoteList("Bearer $token")
            val assets = response.stocks.map {
                AssetItem(
                    ticker = it.stock,
                    lastPrice = it.close ?: 0.0,
                    changePercent = it.change ?: 0.0,
                    logo = it.logo ?: ""
                )
            }
            onResult(assets)
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }
}