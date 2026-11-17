package br.com.rodorush.chartpatterntracker.util.provider.interfaces

import br.com.rodorush.chartpatterntracker.model.AssetItem

interface AssetsProvider {
    /**
     * Carrega a lista de ativos e a entrega em [onResult], vazia quando não há nenhuma.
     *
     * @param useLastLoad `true` aceita, quando a fonte não responde, a lista da última carga,
     * cujos preços são os daquele momento. A tela que mostra o preço como atual fica no padrão
     * `false`.
     */
    suspend fun fetchAssets(useLastLoad: Boolean = false, onResult: (List<AssetItem>) -> Unit)
}
