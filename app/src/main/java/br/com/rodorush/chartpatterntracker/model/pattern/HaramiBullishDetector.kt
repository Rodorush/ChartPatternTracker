package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence

class HaramiBullishDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 2) return occurrences
        for (i in 1 until candlesticks.size) {
            val current = candlesticks[i]
            val previous = candlesticks[i - 1]

            //TODO Confirmar regras do padrão
            val isBullish = current.close > current.open
            val isBearish = previous.open > previous.close
            val isContained = current.open > previous.close && current.close < previous.open
            if (isBullish && isBearish && isContained) {
                occurrences.add(PatternOccurrence(listOf(previous, current)))
            }
        }
        return occurrences
    }
}
