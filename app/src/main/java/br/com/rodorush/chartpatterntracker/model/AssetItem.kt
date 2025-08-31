package br.com.rodorush.chartpatterntracker.model

data class AssetItem(
    val ticker: String,
    val shortName: String = "",
    val longName: String = "",
    // Mantido por compatibilidade e como fallback quando só há 'name' no /quote/list
    val name: String = "",
    val lastPrice: Double = 0.0,
    val changePercent: Double = 0.0,
    val logo: String = ""
)
