package br.com.rodorush.chartpatterntracker.util.provider.mock

import br.com.rodorush.chartpatterntracker.model.PatternItem
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.PatternProvider

class MockPatternProvider : PatternProvider {
    private val mockPatterns = listOf(
        PatternItem(
            id = "1",
            name = mapOf("pt" to "Martelo", "en" to "Hammer"),
            description = mapOf(
                "pt" to "Um pequeno corpo no topo com uma longa sombra inferior, indicando possível reversão de alta.",
                "en" to "A small body at the top with a long lower shadow, indicating a potential bullish reversal."
            ),
            indication = mapOf(
                "pt" to "Reversão Altista",
                "en" to "Bullish Reversal"
            ),
            reliability = mapOf(
                "pt" to "Alta",
                "en" to "High"
            ),
            isChecked = false
        )
    )

    override fun fetchPatterns(onResult: (List<PatternItem>) -> Unit) {
        onResult(mockPatterns)
    }

    override fun fetchPatternById(patternId: String, onResult: (PatternItem?) -> Unit) {
        val pattern = mockPatterns.find { it.id == patternId }
        onResult(pattern)
    }
}