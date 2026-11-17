package br.com.rodorush.chartpatterntracker.util.provider.mock

import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AssetsProvider

class MockAssetsProvider : AssetsProvider {
    override suspend fun fetchAssets(useLastLoad: Boolean, onResult: (List<AssetItem>) -> Unit) {
        val mockAssets = listOf(
            AssetItem(ticker = "PETR4", shortName = "Petrobras PN", longName = "Petróleo Brasileiro S.A. - Petrobras", name = "Petrobras", lastPrice = 38.5, changePercent = 1.2),
            AssetItem(ticker = "VALE3", shortName = "Vale ON", longName = "Vale S.A.", name = "Vale", lastPrice = 62.3, changePercent = -0.8),
            AssetItem(ticker = "ITUB4", shortName = "Itaú Unibanco PN", longName = "Itaú Unibanco Holding S.A.", name = "Itaú Unibanco", lastPrice = 29.1, changePercent = 0.0),
            AssetItem(ticker = "BBDC4", shortName = "Bradesco PN", longName = "Banco Bradesco S.A.", name = "Bradesco", lastPrice = 17.4, changePercent = 2.3)
        )
        onResult(mockAssets)
    }
}
