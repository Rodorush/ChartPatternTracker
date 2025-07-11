package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs

class BebeAbandonadoBaixaDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 3) return occurrences
        for (i in 2 until candlesticks.size) {
            val first = candlesticks[i - 2]
            val second = candlesticks[i - 1]
            val third = candlesticks[i]

            val firstBullish = first.close > first.open
            val secondDoji = abs(second.close - second.open) <= (second.high - second.low) * 0.1f
            val thirdBearish = third.close < third.open

            val gapUp = second.low > first.high
            val gapDown = third.open < second.low
            val closeBelowFirst = third.close < (first.open + first.close) / 2f

            if (firstBullish && secondDoji && thirdBearish && gapUp && gapDown && closeBelowFirst) {
                occurrences.add(PatternOccurrence(listOf(first, second, third)))
            }
        }
        return occurrences
    }
}
