package br.com.rodorush.chartpatterntracker.model

/**
 * Par ativo e tempo gráfico citado num aviso da varredura: o que ficou sem série, o que veio sem
 * velas ou o que foi varrido sobre a série vencida do Room.
 */
data class ScreeningPair(
    val ticker: String,
    val timeframe: TimeframeItem
)
