package br.com.rodorush.chartpatterntracker.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.PatternProvider
import br.com.rodorush.chartpatterntracker.util.LocalPatternProvider

@Composable
fun PatternProviderWrapper(
    patternProvider: PatternProvider,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalPatternProvider provides patternProvider) {
        content()
    }
}