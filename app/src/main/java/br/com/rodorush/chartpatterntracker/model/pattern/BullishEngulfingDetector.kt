package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence

class BullishEngulfingDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 2) return occurrences
        for (i in 1 until candlesticks.size) {
            val current = candlesticks[i]
            val previous = candlesticks[i - 1]
            val prevBearish = previous.open > previous.close
            val currBullish = current.close > current.open
            val engulfs = current.open <= previous.close && current.close >= previous.open
            if (prevBearish && currBullish && engulfs) {
                occurrences.add(PatternOccurrence(listOf(previous, current)))
            }
        }
        return occurrences
    }
}
