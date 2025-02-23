package br.com.rodorush.chartpatterntracker.utils.providers.interfaces

import br.com.rodorush.chartpatterntracker.models.PatternItem

interface PatternProvider {
    fun fetchPatterns(onResult: (List<PatternItem>) -> Unit)
    fun fetchPatternById(patternId: String, onResult: (PatternItem?) -> Unit)
}