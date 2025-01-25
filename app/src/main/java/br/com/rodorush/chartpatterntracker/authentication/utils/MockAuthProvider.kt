package br.com.rodorush.chartpatterntracker.authentication.utils

class MockAuthProvider : AuthProvider {
    override fun signInWithEmailAndPassword(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        // Simula uma autenticação para o preview
        if (email == "test@example.com" && password == "password") {
            onResult(true, null)
        } else {
            onResult(false, "Invalid credentials")
        }
    }

    override fun createUserWithEmailAndPassword(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        // Simula criação de conta
        if (email.contains("@")) {
            onResult(true, null)
        } else {
            onResult(false, "Invalid email format")
        }
    }
}