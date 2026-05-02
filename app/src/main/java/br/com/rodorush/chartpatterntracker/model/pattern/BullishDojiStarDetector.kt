package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Bullish Doji Star (Morning Doji Star) — reversão de alta (Nison).
 * Espelho do Evening Doji Star: 1ª bearish longa, 2ª doji com gap-down completo,
 * 3ª bullish que abre acima do corpo do doji e fecha acima do meio do corpo da 1ª.
 */
class BullishDojiStarDetector : PatternDetector {
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

            val firstBearish = first.close < first.open
            val thirdBullish = third.close > third.open

            val gapDownComplete = second.high < first.low
            val secondBodyHigh = max(second.open, second.close)
            val thirdOpensAboveDojiBody = third.open > secondBodyHigh
            val closeAboveFirstMid = third.close > (first.open + first.close) / 2f

            if (firstBearish && secondIsDoji && thirdBullish && gapDownComplete &&
                thirdOpensAboveDojiBody && closeAboveFirstMid) {
                occurrences.add(PatternOccurrence(listOf(first, second, third)))
            }
        }
        return occurrences
    }

    private companion object {
        const val MAX_DOJI_BODY_RATIO = 0.1f
    }
}
