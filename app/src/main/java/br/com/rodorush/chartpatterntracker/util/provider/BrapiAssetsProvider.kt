package br.com.rodorush.chartpatterntracker.util.provider

import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.api.BrapiApiService
import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AssetsProvider
import br.com.rodorush.customlogs.Log
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
            val stocks = response.stocks
            Log.d(TAG, "Carregados ${stocks.size} ativos do /quote/list")
            val assets = stocks.map { stock ->
                val norm = normalizeName(stock.name)
                AssetItem(
                    ticker = stock.stock,
                    shortName = norm, // será substituído por shortName do Firestore no item visível, quando houver
                    longName = norm,
                    name = norm,
                    lastPrice = stock.close ?: 0.0,
                    changePercent = stock.change ?: 0.0,
                    logo = stock.logo ?: ""
                )
            }
            onResult(assets)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar lista de ativos", e)
            onResult(emptyList())
        }
    }

    private fun normalizeName(s: String?): String {
        if (s.isNullOrBlank()) return ""
        return s.trim().replace("\\s+".toRegex(), " ")
    }

    private companion object {
        const val TAG = "BrapiAssetsProvider"
    }
}
