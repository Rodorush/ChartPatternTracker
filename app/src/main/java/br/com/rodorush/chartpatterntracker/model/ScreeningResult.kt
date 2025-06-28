package br.com.rodorush.chartpatterntracker.model

import br.com.rodorush.chartpatterntracker.R

data class ScreeningResult(
    val pattern: PatternItem,
    val asset: AssetItem,
    val timeframe: TimeframeItem,
    val reliability: String,
    val indication: String,
    val indicationIcon: Int = R.drawable.ic_up_arrow
)