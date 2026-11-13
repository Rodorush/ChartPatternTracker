package br.com.rodorush.chartpatterntracker.model

/** Par ativo e tempo gráfico cuja série não pôde ser obtida durante a varredura. */
data class ScreeningFailure(
    val ticker: String,
    val timeframe: TimeframeItem
)
