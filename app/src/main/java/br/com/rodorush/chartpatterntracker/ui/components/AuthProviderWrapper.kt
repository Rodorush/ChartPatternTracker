package br.com.rodorush.chartpatterntracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import br.com.rodorush.chartpatterntracker.utils.AuthProvider
import br.com.rodorush.chartpatterntracker.utils.LocalAuthProvider

@Composable
fun AuthProviderWrapper(
    authProvider: AuthProvider,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAuthProvider provides authProvider) {
        content()
    }
}