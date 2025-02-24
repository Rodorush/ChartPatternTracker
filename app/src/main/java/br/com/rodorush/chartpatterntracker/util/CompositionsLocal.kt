package br.com.rodorush.chartpatterntracker.util

import androidx.compose.runtime.staticCompositionLocalOf
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import androidx.compose.runtime.compositionLocalOf
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AssetsProvider
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.PatternProvider

// CompositionLocal para acessar o AuthProvider
val LocalAuthProvider = staticCompositionLocalOf<AuthProvider> {
    error("No AuthProvider provided")
}

val LocalPatternProvider = compositionLocalOf<PatternProvider> {
    error("No PatternProvider provided!")
}

val LocalAssetsProvider = staticCompositionLocalOf<AssetsProvider> {
    error("No AssetsProvider provided")
}