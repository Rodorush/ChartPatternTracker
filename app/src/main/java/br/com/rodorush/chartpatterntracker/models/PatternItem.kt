package br.com.rodorush.chartpatterntracker.models

import com.google.firebase.firestore.DocumentId
import java.util.Locale

data class PatternItem(
    @DocumentId
    val id: String = "",
    val name: Map<String, String> = emptyMap(),
    val description: Map<String, String> = emptyMap(),
    val indication: Map<String, String> = emptyMap(),
    val reliability: Map<String, String> = emptyMap(),
    val isChecked: Boolean = false
) {
    fun getLocalized(field: String): String {
        val locale = Locale.getDefault().language
        return when (field.lowercase()) {
            "name" -> name[locale] ?: name["pt"] ?: "Padrão desconhecido"
            "description" -> description[locale] ?: description["pt"] ?: "Descrição desconhecida"
            "indication" -> indication[locale] ?: indication["pt"] ?: "Indicação desconhecida"
            "reliability" -> reliability[locale] ?: reliability["pt"] ?: "Confiança desconhecida"
            else -> "Campo desconhecido"
        }
    }
}