package br.com.rodorush.chartpatterntracker.model

data class AssetItem(
    val ticker: String,
    val lastPrice: Double = 0.0,
    val changePercent: Double = 0.0,
    val logo: String = ""
)
