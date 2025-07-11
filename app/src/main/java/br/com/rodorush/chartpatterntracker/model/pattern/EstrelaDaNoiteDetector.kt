package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs

class EstrelaDaNoiteDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 3) return occurrences
        for (i in 2 until candlesticks.size) {
            val first = candlesticks[i - 2]
            val second = candlesticks[i - 1]
            val third = candlesticks[i]

            val firstBullish = first.close > first.open
            val secondSmall = abs(second.close - second.open) <= abs(first.close - first.open) * 0.5f
            val thirdBearish = third.close < third.open

            val gapUp = second.low > first.high
            val gapDown = third.open < second.close
            val closeBelowMid = third.close < (first.open + first.close) / 2f

            if (firstBullish && secondSmall && thirdBearish && gapUp && gapDown && closeBelowMid) {
                occurrences.add(PatternOccurrence(listOf(first, second, third)))
            }
        }
        return occurrences
    }
}
