package br.com.rodorush.chartpatterntracker.model

import androidx.annotation.StringRes
import br.com.rodorush.chartpatterntracker.R

/**
 * Tempo gráfico oferecido na varredura.
 *
 * @param value intervalo no formato do parâmetro `interval` da Brapi. Também nomeia a
 * subcoleção de velas no Firestore e a série no cache local.
 */
data class TimeframeItem(@StringRes val nameRes: Int, val value: String) {
    companion object {
        /**
         * Só entram intervalos que a Brapi conhece: qualquer outro valor ela recusa com 400.
         * Quais deles respondem depende do plano do token.
         */
        val availableTimeframes = listOf(
            TimeframeItem(R.string.timeframe_1m, "1m"),
            TimeframeItem(R.string.timeframe_2m, "2m"),
            TimeframeItem(R.string.timeframe_5m, "5m"),
            TimeframeItem(R.string.timeframe_15m, "15m"),
            TimeframeItem(R.string.timeframe_30m, "30m"),
            TimeframeItem(R.string.timeframe_1h, "1h"),
            TimeframeItem(R.string.timeframe_1d, "1d"),
            TimeframeItem(R.string.timeframe_1w, "1wk"),
            TimeframeItem(R.string.timeframe_1mo, "1mo")
        )
    }
}
