package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence

/**
 * Three Outside Down — confirmação de Bearish Engulfing seguida de continuação (Nison).
 *   1. 1ª vela bullish.
 *   2. 2ª vela bearish que engulfa estritamente o corpo da 1ª.
 *   3. 3ª vela bearish que fecha abaixo do close da 2ª.
 */
class ThreeOutsideDownDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 3) return occurrences
        for (i in 2 until candlesticks.size) {
            val first = candlesticks[i - 2]
            val second = candlesticks[i - 1]
            val third = candlesticks[i]

            val firstBullish = first.close > first.open
            val secondBearish = second.close < second.open
            val engulfs = second.open > first.close && second.close < first.open
            val thirdBearish = third.close < third.open
            val thirdLower = third.close < second.close

            if (firstBullish && secondBearish && engulfs && thirdBearish && thirdLower) {
                occurrences.add(PatternOccurrence(listOf(first, second, third)))
            }
        }
        return occurrences
    }
}
