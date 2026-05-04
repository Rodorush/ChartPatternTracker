package br.com.rodorush.chartpatterntracker.util.provider.mock

import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider

class MockAuthProvider(
    private var verified: Boolean = false,
    private var email: String? = null,
    private var providerId: String? = "password",
    private var sendVerificationSucceeds: Boolean = true,
    private var reloadFlipsToVerified: Boolean = true
) : AuthProvider {

    var sendEmailVerificationCalls: Int = 0
        private set

    override fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (email == "test@example.com" && password == "password") {
            this.email = email
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
        if (email.contains("@")) {
            this.email = email
            sendEmailVerificationCalls++
            onResult(true, null)
        } else {
            onResult(false, "Invalid email format")
        }
    }

    override fun signInWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit) {
        if (idToken == "mock_token") {
            this.providerId = "google.com"
            this.verified = true
            onResult(true, null)
        } else {
            onResult(false, "Invalid Google token")
        }
    }

    override fun logout(onResult: () -> Unit) {
        email = null
        verified = false
        onResult()
    }

    override fun sendEmailVerification(onResult: (Boolean, String?) -> Unit) {
        sendEmailVerificationCalls++
        if (sendVerificationSucceeds) onResult(true, null) else onResult(false, "send_failed")
    }

    override fun reloadCurrentUser(onResult: (Boolean, String?) -> Unit) {
        if (reloadFlipsToVerified) verified = true
        onResult(true, null)
    }

    override fun isEmailVerified(): Boolean = verified

    override fun currentUserEmail(): String? = email

    override fun currentUserProviderId(): String? = providerId

    fun setVerifiedForTest(value: Boolean) {
        verified = value
    }

    fun setReloadFlipsToVerified(value: Boolean) {
        reloadFlipsToVerified = value
    }

    fun setSendVerificationSucceeds(value: Boolean) {
        sendVerificationSucceeds = value
    }
}
