package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs

/**
 * Abandoned Baby Bearish — reversão de baixa rara (Nison). Variante mais estrita do Evening
 * Doji Star: exige gaps **completos** (sombras isoladas) dos dois lados do doji.
 *   1. 1ª vela bullish.
 *   2. 2ª vela é um doji cujo low fica acima do high da 1ª (gap completo no topo).
 *   3. 3ª vela bearish cujo high fica abaixo do low do doji (gap completo na descida)
 *      e que fecha abaixo do meio do corpo da 1ª.
 */
class AbandonedBabyBearishDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 3) return occurrences
        for (i in 2 until candlesticks.size) {
            val first = candlesticks[i - 2]
            val second = candlesticks[i - 1]
            val third = candlesticks[i]

            val secondRange = second.high - second.low
            if (secondRange <= 0f) continue
            val secondBody = abs(second.close - second.open)
            val secondIsDoji = secondBody <= secondRange * MAX_DOJI_BODY_RATIO

            val firstBullish = first.close > first.open
            val thirdBearish = third.close < third.open

            val gapUpIsland = second.low > first.high
            val gapDownIsland = third.high < second.low
            val closeBelowFirstMid = third.close < (first.open + first.close) / 2f

            if (firstBullish && secondIsDoji && thirdBearish && gapUpIsland &&
                gapDownIsland && closeBelowFirstMid) {
                occurrences.add(PatternOccurrence(listOf(first, second, third)))
            }
        }
        return occurrences
    }

    private companion object {
        const val MAX_DOJI_BODY_RATIO = 0.1f
    }
}
