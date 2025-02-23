package br.com.rodorush.chartpatterntracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import br.com.rodorush.chartpatterntracker.utils.providers.interfaces.PatternProvider
import br.com.rodorush.chartpatterntracker.utils.LocalPatternProvider

@Composable
fun PatternProviderWrapper(
    patternProvider: PatternProvider,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalPatternProvider provides patternProvider) {
        content()
    }
}