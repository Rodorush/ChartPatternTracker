package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence

class ThreeOutsideUpDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 3) return occurrences
        for (i in 2 until candlesticks.size) {
            val first = candlesticks[i - 2]
            val second = candlesticks[i - 1]
            val third = candlesticks[i]

            //TODO Confirmar regras do padrão
            val firstBearish = first.open > first.close
            val secondBullish = second.close > second.open
            val engulfs = second.open < first.close && second.close > first.open
            val thirdHigher = third.close > second.close && third.close > third.open

            if (firstBearish && secondBullish && engulfs && thirdHigher) {
                occurrences.add(PatternOccurrence(listOf(first, second, third)))
            }
        }
        return occurrences
    }
}
