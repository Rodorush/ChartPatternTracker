package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MarteloDeAltaDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        for (candle in candlesticks) {
            val body = abs(candle.close - candle.open)
            val length = candle.high - candle.low
            if (length == 0f) continue
            val lowerShadow = min(candle.open, candle.close) - candle.low
            val upperShadow = candle.high - max(candle.open, candle.close)
            if (body / length < 0.4f && lowerShadow >= body * 2 && upperShadow <= body) {
                occurrences.add(PatternOccurrence(listOf(candle)))
            }
        }
        return occurrences
    }
}
