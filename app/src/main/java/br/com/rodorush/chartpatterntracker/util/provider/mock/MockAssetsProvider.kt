package br.com.rodorush.chartpatterntracker.util.provider.mock

import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AssetsProvider

class MockAssetsProvider : AssetsProvider {
    override suspend fun fetchAssets(onResult: (List<AssetItem>) -> Unit) {
        val mockAssets = listOf(
            AssetItem("PETR4"),
            AssetItem("VALE3"),
            AssetItem("ITUB4"),
            AssetItem("BBDC4")
        )
        onResult(mockAssets)
    }
}