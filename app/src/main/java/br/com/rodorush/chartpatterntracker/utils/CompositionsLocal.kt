package br.com.rodorush.chartpatterntracker.utils

import androidx.compose.runtime.staticCompositionLocalOf
import br.com.rodorush.chartpatterntracker.utils.providers.interfaces.AuthProvider
import androidx.compose.runtime.compositionLocalOf
import br.com.rodorush.chartpatterntracker.utils.providers.interfaces.PatternProvider

// CompositionLocal para acessar o AuthProvider
val LocalAuthProvider = staticCompositionLocalOf<AuthProvider> {
    error("No AuthProvider provided")
}

val LocalPatternProvider = compositionLocalOf<PatternProvider> {
    error("No PatternProvider provided!")
}
