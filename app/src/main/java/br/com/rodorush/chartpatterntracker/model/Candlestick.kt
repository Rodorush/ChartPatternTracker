package br.com.rodorush.chartpatterntracker.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Candlestick(
    val time: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Long? = null
) {
    fun timeAsDateString(): String {
        val date = Date(time)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(date)
    }
}