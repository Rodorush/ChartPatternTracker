package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Evening Doji Star — reversão de baixa (Nison). Variante mais rara do Evening Star em que
 * a estrela é um doji (corpo ≤ 10% do range). Exige gap-up completo entre a 1ª vela e o doji
 * e gap-down entre o corpo do doji e o corpo da 3ª vela.
 */
class EveningDojiStarDetector : PatternDetector {
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

            val gapUpComplete = second.low > first.high
            val secondBodyLow = min(second.open, second.close)
            val thirdOpensBelowDojiBody = third.open < secondBodyLow
            val closeBelowFirstMid = third.close < (first.open + first.close) / 2f

            if (firstBullish && secondIsDoji && thirdBearish && gapUpComplete &&
                thirdOpensBelowDojiBody && closeBelowFirstMid) {
                occurrences.add(PatternOccurrence(listOf(first, second, third)))
            }
        }
        return occurrences
    }

    private companion object {
        const val MAX_DOJI_BODY_RATIO = 0.1f
    }
}
