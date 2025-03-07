package br.com.rodorush.chartpatterntracker.model

data class TimeframeItem(val name: String, val value: String) {
    companion object {
        val availableTimeframes = listOf(
            TimeframeItem("1 Minuto", "1m"),
            TimeframeItem("2 Minutos", "2m"),
            TimeframeItem("3 Minutos", "3m"),
            TimeframeItem("4 Minutos", "4m"),
            TimeframeItem("5 Minutos", "5m"),
            TimeframeItem("6 Minutos", "6m"),
            TimeframeItem("10 Minutos", "10m"),
            TimeframeItem("12 Minutos", "12m"),
            TimeframeItem("15 Minutos", "15m"),
            TimeframeItem("20 Minutos", "20m"),
            TimeframeItem("30 Minutos", "30m"),
            TimeframeItem("1 Hora", "1h"),
            TimeframeItem("2 Horas", "2h"),
            TimeframeItem("3 Horas", "3h"),
            TimeframeItem("4 Horas", "4h"),
            TimeframeItem("6 Horas", "6h"),
            TimeframeItem("8 Horas", "8h"),
            TimeframeItem("12 Horas", "12h"),
            TimeframeItem("Diário", "1d"),
            TimeframeItem("Semanal", "1w"),
            TimeframeItem("Mensal", "1M")
        )
    }
}