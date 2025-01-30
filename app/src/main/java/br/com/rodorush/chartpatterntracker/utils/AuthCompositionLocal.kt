package br.com.rodorush.chartpatterntracker.utils

import androidx.compose.runtime.staticCompositionLocalOf

// CompositionLocal para acessar o AuthProvider
val LocalAuthProvider = staticCompositionLocalOf<AuthProvider> {
    error("No AuthProvider provided")
}