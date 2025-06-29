package br.com.rodorush.chartpatterntracker.model

import androidx.annotation.StringRes
import br.com.rodorush.chartpatterntracker.R

data class TimeframeItem(@StringRes val nameRes: Int, val value: String) {
    companion object {
        val availableTimeframes = listOf(
            TimeframeItem(R.string.timeframe_1m, "1m"),
            TimeframeItem(R.string.timeframe_2m, "2m"),
            TimeframeItem(R.string.timeframe_3m, "3m"),
            TimeframeItem(R.string.timeframe_4m, "4m"),
            TimeframeItem(R.string.timeframe_5m, "5m"),
            TimeframeItem(R.string.timeframe_6m, "6m"),
            TimeframeItem(R.string.timeframe_10m, "10m"),
            TimeframeItem(R.string.timeframe_12m, "12m"),
            TimeframeItem(R.string.timeframe_15m, "15m"),
            TimeframeItem(R.string.timeframe_20m, "20m"),
            TimeframeItem(R.string.timeframe_30m, "30m"),
            TimeframeItem(R.string.timeframe_1h, "1h"),
            TimeframeItem(R.string.timeframe_2h, "2h"),
            TimeframeItem(R.string.timeframe_3h, "3h"),
            TimeframeItem(R.string.timeframe_4h, "4h"),
            TimeframeItem(R.string.timeframe_6h, "6h"),
            TimeframeItem(R.string.timeframe_8h, "8h"),
            TimeframeItem(R.string.timeframe_12h, "12h"),
            TimeframeItem(R.string.timeframe_1d, "1d"),
            TimeframeItem(R.string.timeframe_1w, "1w"),
            TimeframeItem(R.string.timeframe_1mo, "1M")
        )
    }
}
