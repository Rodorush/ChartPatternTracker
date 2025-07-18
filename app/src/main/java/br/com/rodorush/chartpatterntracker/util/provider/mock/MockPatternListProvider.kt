package br.com.rodorush.chartpatterntracker.util.provider.mock

import br.com.rodorush.chartpatterntracker.model.PatternItem
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.PatternProvider

class MockPatternListProvider : PatternProvider {
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
        ),
        PatternItem(
            id = "2",
            name = mapOf("pt" to "Estrela Cadente", "en" to "Shooting Star"),
            description = mapOf(
                "pt" to "Um pequeno corpo na base com uma longa sombra superior, indicando possível reversão de baixa.",
                "en" to "A small body at the bottom with a long upper shadow, indicating a potential bearish reversal."
            ),
            indication = mapOf(
                "pt" to "Reversão Baixista",
                "en" to "Bearish Reversal"
            ),
            reliability = mapOf(
                "pt" to "Média",
                "en" to "Medium"
            ),
            isChecked = false
        ),
        PatternItem(
            id = "3",
            name = mapOf("pt" to "Engolfo de Baixa", "en" to "Bearish Engulfing"),
            description = mapOf(
                "pt" to "Um grande candle de baixa engolfa completamente o candle de alta anterior, sinalizando reversão.",
                "en" to "A large bearish candle completely engulfs the previous bullish candle, signaling a reversal."
            ),
            indication = mapOf(
                "pt" to "Reversão Baixista",
                "en" to "Bearish Reversal"
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