package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs

/**
 * Harami Bullish — reversão de alta (Nison, *Japanese Candlestick Charting Techniques*).
 *   1. Vela anterior bearish com corpo dominante (corpo ≥ 60% do range total da vela).
 *   2. Vela atual bullish (close > open) cujo corpo está totalmente contido no corpo da anterior.
 */
class HaramiBullishDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 2) return occurrences
        for (i in 1 until candlesticks.size) {
            val previous = candlesticks[i - 1]
            val current = candlesticks[i]

            val previousRange = previous.high - previous.low
            if (previousRange <= 0f) continue
            val previousBody = abs(previous.open - previous.close)
            val previousBearish = previous.close < previous.open
            val hasStrongBody = previousBearish && previousBody / previousRange >= MIN_PREVIOUS_BODY_RATIO

            val currentBullish = current.close > current.open
            val containedInPreviousBody = current.open > previous.close && current.close < previous.open

            if (hasStrongBody && currentBullish && containedInPreviousBody) {
                occurrences.add(PatternOccurrence(listOf(previous, current)))
            }
        }
        return occurrences
    }

    private companion object {
        const val MIN_PREVIOUS_BODY_RATIO = 0.6f
    }
}
