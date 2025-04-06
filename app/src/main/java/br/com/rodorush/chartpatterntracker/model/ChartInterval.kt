package br.com.rodorush.chartpatterntracker.model

enum class ChartInterval(val value: String) {
    DAILY("1d"),
    MINUTE_1("1m"),
    MINUTE_5("5m"),
    MINUTE_15("15m"),
    MINUTE_30("30m"),
    HOUR_1("1h");

    companion object {
        fun fromString(value: String): ChartInterval {
            return entries.find { it.value == value } ?: DAILY
        }
    }
}