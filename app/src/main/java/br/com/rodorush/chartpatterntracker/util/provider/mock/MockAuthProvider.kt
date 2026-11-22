package br.com.rodorush.chartpatterntracker.util.provider.mock

import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider

class MockAuthProvider(
    private var verified: Boolean = false,
    private var email: String? = null,
    private var displayName: String? = null,
    private var providerIds: List<String> = listOf(AuthProvider.PASSWORD_PROVIDER),
    // Com mais de um provedor vinculado, a ordem não diz por qual a sessão foi aberta.
    private var signInProvider: String? = providerIds.singleOrNull(),
    private var sendVerificationSucceeds: Boolean = true,
    private var reloadFlipsToVerified: Boolean = true,
    private var sendPasswordResetSucceeds: Boolean = true,
    private var deleteSucceeds: Boolean = true,
    private var deleteRequiresRecentLogin: Boolean = false,
    private var reauthSucceeds: Boolean = true,
    private var updatePasswordSucceeds: Boolean = true,
    private var updatePasswordRequiresRecentLogin: Boolean = false,
    private var updateEmailSucceeds: Boolean = true,
    private var updateEmailRequiresRecentLogin: Boolean = false,
    private var updateNameSucceeds: Boolean = true,
    private var uid: String? = "mock-uid"
) : AuthProvider {

    private var reloadSucceeds: Boolean = true
    private var reloadErrorCode: String? = null

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

    var updatePasswordCalls: Int = 0
        private set
    var verifyBeforeUpdateEmailCalls: Int = 0
        private set
    var lastNewEmail: String? = null
        private set
    var updateDisplayNameCalls: Int = 0
        private set

    override fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (email == "test@example.com" && password == "password") {
            this.email = email
            if (AuthProvider.PASSWORD_PROVIDER !in providerIds) providerIds = providerIds + AuthProvider.PASSWORD_PROVIDER
            this.signInProvider = AuthProvider.PASSWORD_PROVIDER
            onResult(true, null)
        } else {
            onResult(false, AuthProvider.INVALID_CREDENTIAL)
        }
    }

    override fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (email.contains("@")) {
            this.email = email
            this.displayName = displayName
            sendEmailVerificationCalls++
            onResult(true, null)
        } else {
            // Como no FirebaseAuthProvider, e-mail malformado fica sem código.
            onResult(false, null)
        }
    }

    override fun signInWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit) {
        if (idToken == "mock_token") {
            if (AuthProvider.GOOGLE_PROVIDER !in providerIds) providerIds = providerIds + AuthProvider.GOOGLE_PROVIDER
            this.signInProvider = AuthProvider.GOOGLE_PROVIDER
            this.verified = true
            onResult(true, null)
        } else {
            onResult(false, if (idToken == null) AuthProvider.MISSING_ID_TOKEN else AuthProvider.INVALID_CREDENTIAL)
        }
    }

    override fun logout(onResult: () -> Unit) {
        email = null
        verified = false
        onResult()
    }

    override fun sendEmailVerification(onResult: (Boolean, String?) -> Unit) {
        sendEmailVerificationCalls++
        if (sendVerificationSucceeds) onResult(true, null) else onResult(false, null)
    }

    override fun reloadCurrentUser(onResult: (Boolean, String?) -> Unit) {
        if (!reloadSucceeds) {
            onResult(false, reloadErrorCode)
            return
        }
        if (reloadFlipsToVerified) verified = true
        onResult(true, null)
    }

    override fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        sendPasswordResetEmailCalls++
        lastPasswordResetEmail = email
        if (sendPasswordResetSucceeds) onResult(true, null) else onResult(false, null)
    }

    override fun deleteCurrentUser(onResult: (Boolean, String?) -> Unit) {
        deleteCalls++
        when {
            deleteRequiresRecentLogin -> onResult(false, AuthProvider.REQUIRES_RECENT_LOGIN)
            deleteSucceeds -> {
                email = null
                verified = false
                onResult(true, null)
            }
            else -> onResult(false, null)
        }
    }

    override fun reauthenticateWithPassword(
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        reauthCalls++
        if (reauthSucceeds) {
            // Sessão renovada: as operações sensíveis não exigem mais login recente.
            clearRecentLoginFlags()
            signInProvider = AuthProvider.PASSWORD_PROVIDER
            onResult(true, null)
        } else {
            onResult(false, AuthProvider.INVALID_CREDENTIAL)
        }
    }

    override fun reauthenticateWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit) {
        reauthCalls++
        if (reauthSucceeds && idToken != null) {
            clearRecentLoginFlags()
            signInProvider = AuthProvider.GOOGLE_PROVIDER
            onResult(true, null)
        } else {
            onResult(false, if (idToken == null) AuthProvider.MISSING_ID_TOKEN else AuthProvider.INVALID_CREDENTIAL)
        }
    }

    override fun updatePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        updatePasswordCalls++
        when {
            updatePasswordRequiresRecentLogin -> onResult(false, AuthProvider.REQUIRES_RECENT_LOGIN)
            updatePasswordSucceeds -> onResult(true, null)
            else -> onResult(false, null)
        }
    }

    override fun verifyBeforeUpdateEmail(newEmail: String, onResult: (Boolean, String?) -> Unit) {
        verifyBeforeUpdateEmailCalls++
        lastNewEmail = newEmail
        when {
            updateEmailRequiresRecentLogin -> onResult(false, AuthProvider.REQUIRES_RECENT_LOGIN)
            updateEmailSucceeds -> onResult(true, null)
            else -> onResult(false, null)
        }
    }

    override fun updateDisplayName(displayName: String, onResult: (Boolean, String?) -> Unit) {
        updateDisplayNameCalls++
        if (updateNameSucceeds) {
            this.displayName = displayName
            onResult(true, null)
        } else {
            onResult(false, null)
        }
    }

    private fun clearRecentLoginFlags() {
        deleteRequiresRecentLogin = false
        updatePasswordRequiresRecentLogin = false
        updateEmailRequiresRecentLogin = false
    }

    override fun isEmailVerified(): Boolean = verified

    override fun currentUserId(): String? = uid

    override fun currentUserEmail(): String? = email

    override fun currentUserDisplayName(): String? = displayName

    override fun currentUserProviderIds(): List<String> = providerIds

    override fun currentSignInProvider(onResult: (String?) -> Unit) = onResult(signInProvider)

    fun setSignInProviderForTest(value: String?) {
        signInProvider = value
    }

    fun setVerifiedForTest(value: Boolean) {
        verified = value
    }

    fun setReloadFlipsToVerified(value: Boolean) {
        reloadFlipsToVerified = value
    }

    /** Com [value] `false`, o `reloadCurrentUser` falha com o [errorCode] do [AuthProvider]. */
    fun setReloadSucceeds(value: Boolean, errorCode: String? = null) {
        reloadSucceeds = value
        reloadErrorCode = errorCode
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

    fun setUpdatePasswordSucceeds(value: Boolean) {
        updatePasswordSucceeds = value
    }

    fun setUpdatePasswordRequiresRecentLogin(value: Boolean) {
        updatePasswordRequiresRecentLogin = value
    }

    fun setUpdateEmailSucceeds(value: Boolean) {
        updateEmailSucceeds = value
    }

    fun setUpdateEmailRequiresRecentLogin(value: Boolean) {
        updateEmailRequiresRecentLogin = value
    }

    fun setUpdateNameSucceeds(value: Boolean) {
        updateNameSucceeds = value
    }
}
