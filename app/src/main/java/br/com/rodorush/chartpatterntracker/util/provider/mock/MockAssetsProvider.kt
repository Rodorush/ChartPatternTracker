package br.com.rodorush.chartpatterntracker.util.provider.mock

import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AssetsProvider

class MockAssetsProvider : AssetsProvider {
    override suspend fun fetchAssets(onResult: (List<AssetItem>) -> Unit) {
        val mockAssets = listOf(
            AssetItem("PETR4", 38.5, 1.2),
            AssetItem("VALE3", 62.3, -0.8),
            AssetItem("ITUB4", 29.1, 0.0),
            AssetItem("BBDC4", 17.4, 2.3)
        )
        onResult(mockAssets)
    }
}