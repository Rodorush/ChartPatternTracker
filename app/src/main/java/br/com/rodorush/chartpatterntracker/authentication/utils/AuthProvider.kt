package br.com.rodorush.chartpatterntracker.authentication.utils

// Interface para o provedor de autenticação
interface AuthProvider {
    fun signInWithEmailAndPassword(email: String, password: String, onResult: (Boolean, String?) -> Unit)
}