package br.com.rodorush.chartpatterntracker.util.provider.interfaces

import br.com.rodorush.chartpatterntracker.model.PatternItem

interface PatternProvider {
    fun fetchPatterns(onResult: (List<PatternItem>) -> Unit)
    fun fetchPatternById(patternId: String, onResult: (PatternItem?) -> Unit)
}