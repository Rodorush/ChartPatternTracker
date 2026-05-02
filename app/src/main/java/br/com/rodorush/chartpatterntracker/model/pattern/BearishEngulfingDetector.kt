package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence

/**
 * Bearish Engulfing — reversão de baixa.
 * Regra (Nison, *Japanese Candlestick Charting Techniques*):
 *   1. Vela anterior bullish (close > open).
 *   2. Vela atual bearish (close < open) cujo corpo engloba estritamente o corpo da anterior
 *      (open atual > close anterior **e** close atual < open anterior).
 */
class BearishEngulfingDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 2) return occurrences
        for (i in 1 until candlesticks.size) {
            val previous = candlesticks[i - 1]
            val current = candlesticks[i]

            val prevBullish = previous.close > previous.open
            val currBearish = current.close < current.open
            val engulfs = current.open > previous.close && current.close < previous.open
            if (prevBullish && currBearish && engulfs) {
                occurrences.add(PatternOccurrence(listOf(previous, current)))
            }
        }
        return occurrences
    }
}
