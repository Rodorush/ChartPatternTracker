package br.com.rodorush.chartpatterntracker.utils.providers

import br.com.rodorush.chartpatterntracker.ui.screens.PatternItem

interface PatternProvider {
    fun fetchPatterns(onResult: (List<PatternItem>) -> Unit)
}

class FirebasePatternProvider : PatternProvider {
    override fun fetchPatterns(onResult: (List<PatternItem>) -> Unit) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("candlestick_patterns")
            .get()
            .addOnSuccessListener { documents ->
                val patterns = documents.mapNotNull { it.toObject(PatternItem::class.java) }
                onResult(patterns)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}

class MockPatternProvider : PatternProvider {
    override fun fetchPatterns(onResult: (List<PatternItem>) -> Unit) {
        onResult(
            listOf(
                PatternItem(id = "1", name = mapOf("pt" to "Martelo", "en" to "Hammer")),
                PatternItem(id = "2", name = mapOf("pt" to "Estrela Cadente", "en" to "Shooting Star")),
                PatternItem(id = "3", name = mapOf("pt" to "Engolfo de Alta", "en" to "Bullish Engulfing"))
            )
        )
    }
}