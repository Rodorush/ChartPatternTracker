package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs

class BullishDojiStarDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 3) return occurrences
        for (i in 2 until candlesticks.size) {
            val first = candlesticks[i - 2]
            val second = candlesticks[i - 1]
            val third = candlesticks[i]

            //TODO Confirmar regras do padrão
            val firstBearish = first.open > first.close
            val secondDoji = abs(second.close - second.open) <= (second.high - second.low) * 0.1f
            val thirdBullish = third.close > third.open

            val gapDown = second.high < first.low
            val gapUp = third.open > second.high
            val closeAboveMid = third.close > (first.open + first.close) / 2f

            if (firstBearish && secondDoji && thirdBullish && gapDown && gapUp && closeAboveMid) {
                occurrences.add(PatternOccurrence(listOf(first, second, third)))
            }
        }
        return occurrences
    }
}
