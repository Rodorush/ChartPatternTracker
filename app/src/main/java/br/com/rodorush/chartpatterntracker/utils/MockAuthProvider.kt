package br.com.rodorush.chartpatterntracker.utils

class MockAuthProvider : AuthProvider {
    override fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        // Simula uma autenticação para o preview
        if (email == "test@example.com" && password == "password") {
            onResult(true, null)
        } else {
            onResult(false, "Invalid credentials")
        }
    }

    override fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        // Simula criação de conta
        if (email.contains("@")) {
            onResult(true, null)
        } else {
            onResult(false, "Invalid email format")
        }
    }

    override fun signInWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit) {
        // Simula um login bem-sucedido para testes
        if (idToken == "mock_token") {
            onResult(true, null)
        } else {
            onResult(false, "Invalid Google token")
        }
    }
}