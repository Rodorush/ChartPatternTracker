package br.com.rodorush.chartpatterntracker.model

data class ScreeningResult(
    val pattern: PatternItem,
    val asset: AssetItem,
    val timeframe: TimeframeItem,
    val reliability: String,
    val indication: String,
    val indicationIcon: Int
)
