package br.com.rodorush.chartpatterntracker.util.provider

import br.com.rodorush.chartpatterntracker.model.PatternItem
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.PatternProvider
import com.google.firebase.firestore.FirebaseFirestore

class FirebasePatternProvider : PatternProvider {
    override fun fetchPatterns(onResult: (List<PatternItem>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("candlestick_patterns")
            .get()
            .addOnSuccessListener { documents ->
                val patterns = documents.mapNotNull { it.toObject(PatternItem::class.java) }
                onResult(patterns)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    override fun fetchPatternById(patternId: String, onResult: (PatternItem?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("candlestick_patterns")
            .document(patternId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val pattern = document.toObject(PatternItem::class.java)
                    onResult(pattern)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
    }
}