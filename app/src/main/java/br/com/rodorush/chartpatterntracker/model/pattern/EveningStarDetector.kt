package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Evening Star — reversão de baixa (Nison, *Japanese Candlestick Charting Techniques*).
 *   1. Primeira vela bullish com corpo longo.
 *   2. Segunda vela (estrela) com corpo pequeno (≤ 50% do corpo da 1ª) e gap-up entre o corpo.
 *   3. Terceira vela bearish que abre dentro/abaixo do corpo da estrela e fecha abaixo do meio do corpo da 1ª.
 */
class EveningStarDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 3) return occurrences
        for (i in 2 until candlesticks.size) {
            val first = candlesticks[i - 2]
            val second = candlesticks[i - 1]
            val third = candlesticks[i]

            val firstBody = abs(first.close - first.open)
            if (firstBody <= 0f) continue
            val firstBullish = first.close > first.open
            val secondBody = abs(second.close - second.open)
            val secondSmall = secondBody <= firstBody * MAX_STAR_BODY_RATIO
            val thirdBearish = third.close < third.open

            val secondBodyLow = min(second.open, second.close)
            val firstBodyHigh = max(first.open, first.close)
            val gapUpBetweenBodies = secondBodyLow > firstBodyHigh
            val thirdOpensWithinSecondBody = third.open <= max(second.open, second.close)
            val closeBelowFirstMid = third.close < (first.open + first.close) / 2f

            if (firstBullish && secondSmall && thirdBearish && gapUpBetweenBodies &&
                thirdOpensWithinSecondBody && closeBelowFirstMid) {
                occurrences.add(PatternOccurrence(listOf(first, second, third)))
            }
        }
        return occurrences
    }

    private companion object {
        const val MAX_STAR_BODY_RATIO = 0.5f
    }
}
