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
}