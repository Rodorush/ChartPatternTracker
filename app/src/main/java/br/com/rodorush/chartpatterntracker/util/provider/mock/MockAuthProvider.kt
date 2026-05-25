package br.com.rodorush.chartpatterntracker.util.provider.mock

import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider

class MockAuthProvider(
    private var verified: Boolean = false,
    private var email: String? = null,
    private var providerId: String? = "password",
    private var sendVerificationSucceeds: Boolean = true,
    private var reloadFlipsToVerified: Boolean = true,
    private var sendPasswordResetSucceeds: Boolean = true,
    private var deleteSucceeds: Boolean = true,
    private var deleteRequiresRecentLogin: Boolean = false,
    private var reauthSucceeds: Boolean = true
) : AuthProvider {

    var sendEmailVerificationCalls: Int = 0
        private set

    var sendPasswordResetEmailCalls: Int = 0
        private set
    var lastPasswordResetEmail: String? = null
        private set

    var deleteCalls: Int = 0
        private set
    var reauthCalls: Int = 0
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

    override fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        sendPasswordResetEmailCalls++
        lastPasswordResetEmail = email
        if (sendPasswordResetSucceeds) onResult(true, null) else onResult(false, "send_failed")
    }

    override fun deleteCurrentUser(onResult: (Boolean, String?) -> Unit) {
        deleteCalls++
        when {
            deleteRequiresRecentLogin -> onResult(false, "requires-recent-login")
            deleteSucceeds -> {
                email = null
                verified = false
                onResult(true, null)
            }
            else -> onResult(false, "delete_failed")
        }
    }

    override fun reauthenticateWithPassword(
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        reauthCalls++
        if (reauthSucceeds) {
            // Sessão renovada: o próximo delete não exige mais login recente.
            deleteRequiresRecentLogin = false
            onResult(true, null)
        } else {
            onResult(false, "reauth_failed")
        }
    }

    override fun reauthenticateWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit) {
        reauthCalls++
        if (reauthSucceeds && idToken != null) {
            deleteRequiresRecentLogin = false
            onResult(true, null)
        } else {
            onResult(false, "reauth_failed")
        }
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

    fun setSendPasswordResetSucceeds(value: Boolean) {
        sendPasswordResetSucceeds = value
    }

    fun setDeleteSucceeds(value: Boolean) {
        deleteSucceeds = value
    }

    fun setDeleteRequiresRecentLogin(value: Boolean) {
        deleteRequiresRecentLogin = value
    }

    fun setReauthSucceeds(value: Boolean) {
        reauthSucceeds = value
    }
}
