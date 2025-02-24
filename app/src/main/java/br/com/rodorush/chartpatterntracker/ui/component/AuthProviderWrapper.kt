package br.com.rodorush.chartpatterntracker.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import br.com.rodorush.chartpatterntracker.util.LocalAuthProvider

@Composable
fun AuthProviderWrapper(
    authProvider: AuthProvider,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAuthProvider provides authProvider) {
        content()
    }
}