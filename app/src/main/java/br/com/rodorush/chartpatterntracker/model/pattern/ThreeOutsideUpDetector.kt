package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence

/**
 * Three Outside Up — confirmação de Bullish Engulfing seguida de continuação (Nison).
 *   1. 1ª vela bearish.
 *   2. 2ª vela bullish que engulfa estritamente o corpo da 1ª.
 *   3. 3ª vela bullish que fecha acima do close da 2ª.
 */
class ThreeOutsideUpDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 3) return occurrences
        for (i in 2 until candlesticks.size) {
            val first = candlesticks[i - 2]
            val second = candlesticks[i - 1]
            val third = candlesticks[i]

            val firstBearish = first.open > first.close
            val secondBullish = second.close > second.open
            val engulfs = second.open < first.close && second.close > first.open
            val thirdBullish = third.close > third.open
            val thirdHigher = third.close > second.close

            if (firstBearish && secondBullish && engulfs && thirdBullish && thirdHigher) {
                occurrences.add(PatternOccurrence(listOf(first, second, third)))
            }
        }
        return occurrences
    }
}
