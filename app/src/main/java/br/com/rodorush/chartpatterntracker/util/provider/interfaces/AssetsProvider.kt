package br.com.rodorush.chartpatterntracker.util.provider.interfaces

import br.com.rodorush.chartpatterntracker.model.AssetItem

interface AssetsProvider {
    suspend fun fetchAssets(onResult: (List<AssetItem>) -> Unit)
}