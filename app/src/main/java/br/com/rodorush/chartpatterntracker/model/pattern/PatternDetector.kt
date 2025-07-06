package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence

interface PatternDetector {
    fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence>
}
