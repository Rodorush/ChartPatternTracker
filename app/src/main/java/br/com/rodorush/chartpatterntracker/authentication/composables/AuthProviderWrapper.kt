package br.com.rodorush.chartpatterntracker.authentication.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import br.com.rodorush.chartpatterntracker.authentication.utils.AuthProvider
import br.com.rodorush.chartpatterntracker.authentication.utils.LocalAuthProvider

@Composable
fun AuthProviderWrapper(
    authProvider: AuthProvider,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAuthProvider provides authProvider) {
        content()
    }
}