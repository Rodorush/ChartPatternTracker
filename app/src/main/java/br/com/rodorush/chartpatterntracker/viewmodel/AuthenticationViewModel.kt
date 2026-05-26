package br.com.rodorush.chartpatterntracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import br.com.rodorush.customlogs.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val cooldownTickMillis: Long = 1000L,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    sealed class VerificationStatus {
        data object Idle : VerificationStatus()
        data object Sending : VerificationStatus()
        data object Sent : VerificationStatus()
        data class SendError(val message: String?) : VerificationStatus()
        data object Verified : VerificationStatus()
        data object NotYetVerified : VerificationStatus()
    }

    sealed class ResetPasswordStatus {
        data object Idle : ResetPasswordStatus()
        data object Sending : ResetPasswordStatus()
        data object Sent : ResetPasswordStatus()
        data class Error(val message: String?) : ResetPasswordStatus()
    }

    sealed class DeleteAccountStatus {
        data object Idle : DeleteAccountStatus()
        data object Deleting : DeleteAccountStatus()
        data object Deleted : DeleteAccountStatus()
        data object RequiresReauth : DeleteAccountStatus()
        data class Error(val message: String?) : DeleteAccountStatus()
    }

    sealed class UpdateNameStatus {
        data object Idle : UpdateNameStatus()
        data object Updating : UpdateNameStatus()
        data object Updated : UpdateNameStatus()
        data class Error(val message: String?) : UpdateNameStatus()
    }

    sealed class UpdatePasswordStatus {
        data object Idle : UpdatePasswordStatus()
        data object Updating : UpdatePasswordStatus()
        data object Updated : UpdatePasswordStatus()
        data object RequiresReauth : UpdatePasswordStatus()
        data class Error(val message: String?) : UpdatePasswordStatus()
    }

    sealed class UpdateEmailStatus {
        data object Idle : UpdateEmailStatus()
        data object Updating : UpdateEmailStatus()
        // Sucesso = link de verificação enviado ao novo e-mail; a troca só
        // efetiva após o clique. O e-mail atual permanece até lá.
        data object VerificationSent : UpdateEmailStatus()
        data object RequiresReauth : UpdateEmailStatus()
        data class Error(val message: String?) : UpdateEmailStatus()
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _resendCooldownSeconds = MutableStateFlow(0)
    val resendCooldownSeconds = _resendCooldownSeconds.asStateFlow()

    private val _verificationStatus = MutableStateFlow<VerificationStatus>(VerificationStatus.Idle)
    val verificationStatus = _verificationStatus.asStateFlow()

    private val _isCheckingVerification = MutableStateFlow(false)
    val isCheckingVerification = _isCheckingVerification.asStateFlow()

    private val _resetPasswordStatus = MutableStateFlow<ResetPasswordStatus>(ResetPasswordStatus.Idle)
    val resetPasswordStatus = _resetPasswordStatus.asStateFlow()

    private val _deleteAccountStatus = MutableStateFlow<DeleteAccountStatus>(DeleteAccountStatus.Idle)
    val deleteAccountStatus = _deleteAccountStatus.asStateFlow()

    private val _updateNameStatus = MutableStateFlow<UpdateNameStatus>(UpdateNameStatus.Idle)
    val updateNameStatus = _updateNameStatus.asStateFlow()

    private val _updatePasswordStatus = MutableStateFlow<UpdatePasswordStatus>(UpdatePasswordStatus.Idle)
    val updatePasswordStatus = _updatePasswordStatus.asStateFlow()

    private val _updateEmailStatus = MutableStateFlow<UpdateEmailStatus>(UpdateEmailStatus.Idle)
    val updateEmailStatus = _updateEmailStatus.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        getData()
    }

    private fun getData() {
        viewModelScope.launch {
            _isLoading.value = false
        }
    }

    fun armResendCooldown(seconds: Int = COOLDOWN_SECONDS) {
        cooldownJob?.cancel()
        _resendCooldownSeconds.value = seconds
        cooldownJob = viewModelScope.launch(ioDispatcher) {
            while (_resendCooldownSeconds.value > 0) {
                delay(cooldownTickMillis)
                _resendCooldownSeconds.value = (_resendCooldownSeconds.value - 1).coerceAtLeast(0)
            }
        }
    }

    fun sendVerificationEmail(authProvider: AuthProvider) {
        if (_resendCooldownSeconds.value > 0) return
        _verificationStatus.value = VerificationStatus.Sending
        authProvider.sendEmailVerification { success, error ->
            if (success) {
                _verificationStatus.value = VerificationStatus.Sent
                armResendCooldown()
            } else {
                Log.w(TAG, "sendVerificationEmail falhou: $error")
                _verificationStatus.value = VerificationStatus.SendError(error)
            }
        }
    }

    fun checkVerified(authProvider: AuthProvider) {
        if (_isCheckingVerification.value) return
        _isCheckingVerification.value = true
        authProvider.reloadCurrentUser { success, error ->
            _isCheckingVerification.value = false
            if (!success) {
                Log.w(TAG, "checkVerified.reload falhou: $error")
                _verificationStatus.value = VerificationStatus.SendError(error)
                return@reloadCurrentUser
            }
            _verificationStatus.value = if (authProvider.isEmailVerified()) {
                VerificationStatus.Verified
            } else {
                VerificationStatus.NotYetVerified
            }
        }
    }

    fun resetVerificationStatus() {
        _verificationStatus.value = VerificationStatus.Idle
    }

    fun sendPasswordReset(email: String, authProvider: AuthProvider) {
        if (_resendCooldownSeconds.value > 0) return
        _resetPasswordStatus.value = ResetPasswordStatus.Sending
        authProvider.sendPasswordResetEmail(email) { success, error ->
            if (success) {
                _resetPasswordStatus.value = ResetPasswordStatus.Sent
                armResendCooldown()
            } else {
                Log.w(TAG, "sendPasswordReset falhou: $error")
                _resetPasswordStatus.value = ResetPasswordStatus.Error(error)
            }
        }
    }

    fun resetPasswordResetStatus() {
        _resetPasswordStatus.value = ResetPasswordStatus.Idle
    }

    fun deleteAccount(authProvider: AuthProvider) {
        _deleteAccountStatus.value = DeleteAccountStatus.Deleting
        authProvider.deleteCurrentUser { success, error ->
            when {
                success -> _deleteAccountStatus.value = DeleteAccountStatus.Deleted
                requiresRecentLogin(error) ->
                    _deleteAccountStatus.value = DeleteAccountStatus.RequiresReauth
                else -> {
                    Log.w(TAG, "deleteAccount falhou: $error")
                    _deleteAccountStatus.value = DeleteAccountStatus.Error(error)
                }
            }
        }
    }

    fun reauthenticateAndDeleteWithPassword(password: String, authProvider: AuthProvider) {
        _deleteAccountStatus.value = DeleteAccountStatus.Deleting
        authProvider.reauthenticateWithPassword(password) { success, error ->
            if (success) {
                deleteAccount(authProvider)
            } else {
                Log.w(TAG, "reauthenticateWithPassword falhou: $error")
                _deleteAccountStatus.value = DeleteAccountStatus.Error(error)
            }
        }
    }

    fun reauthenticateAndDeleteWithGoogle(idToken: String?, authProvider: AuthProvider) {
        _deleteAccountStatus.value = DeleteAccountStatus.Deleting
        authProvider.reauthenticateWithGoogle(idToken) { success, error ->
            if (success) {
                deleteAccount(authProvider)
            } else {
                Log.w(TAG, "reauthenticateWithGoogle falhou: $error")
                _deleteAccountStatus.value = DeleteAccountStatus.Error(error)
            }
        }
    }

    fun resetDeleteAccountStatus() {
        _deleteAccountStatus.value = DeleteAccountStatus.Idle
    }

    // --- NF-03d: alterar nome de exibição (não exige sessão recente) ---

    fun updateDisplayName(displayName: String, authProvider: AuthProvider) {
        _updateNameStatus.value = UpdateNameStatus.Updating
        authProvider.updateDisplayName(displayName) { success, error ->
            if (success) {
                _updateNameStatus.value = UpdateNameStatus.Updated
            } else {
                Log.w(TAG, "updateDisplayName falhou: $error")
                _updateNameStatus.value = UpdateNameStatus.Error(error)
            }
        }
    }

    fun resetUpdateNameStatus() {
        _updateNameStatus.value = UpdateNameStatus.Idle
    }

    // --- NF-03b: alterar senha (exige sessão recente) ---

    fun updatePassword(newPassword: String, authProvider: AuthProvider) {
        _updatePasswordStatus.value = UpdatePasswordStatus.Updating
        authProvider.updatePassword(newPassword) { success, error ->
            when {
                success -> _updatePasswordStatus.value = UpdatePasswordStatus.Updated
                requiresRecentLogin(error) ->
                    _updatePasswordStatus.value = UpdatePasswordStatus.RequiresReauth
                else -> {
                    Log.w(TAG, "updatePassword falhou: $error")
                    _updatePasswordStatus.value = UpdatePasswordStatus.Error(error)
                }
            }
        }
    }

    fun reauthAndUpdatePasswordWithPassword(
        currentPassword: String,
        newPassword: String,
        authProvider: AuthProvider
    ) {
        _updatePasswordStatus.value = UpdatePasswordStatus.Updating
        authProvider.reauthenticateWithPassword(currentPassword) { success, error ->
            if (success) {
                updatePassword(newPassword, authProvider)
            } else {
                Log.w(TAG, "reauthenticateWithPassword (updatePassword) falhou: $error")
                _updatePasswordStatus.value = UpdatePasswordStatus.Error(error)
            }
        }
    }

    fun reauthAndUpdatePasswordWithGoogle(
        idToken: String?,
        newPassword: String,
        authProvider: AuthProvider
    ) {
        _updatePasswordStatus.value = UpdatePasswordStatus.Updating
        authProvider.reauthenticateWithGoogle(idToken) { success, error ->
            if (success) {
                updatePassword(newPassword, authProvider)
            } else {
                Log.w(TAG, "reauthenticateWithGoogle (updatePassword) falhou: $error")
                _updatePasswordStatus.value = UpdatePasswordStatus.Error(error)
            }
        }
    }

    fun resetUpdatePasswordStatus() {
        _updatePasswordStatus.value = UpdatePasswordStatus.Idle
    }

    // --- NF-03c: alterar e-mail via verifyBeforeUpdateEmail (exige sessão recente) ---

    fun updateEmail(newEmail: String, authProvider: AuthProvider) {
        _updateEmailStatus.value = UpdateEmailStatus.Updating
        authProvider.verifyBeforeUpdateEmail(newEmail) { success, error ->
            when {
                success -> _updateEmailStatus.value = UpdateEmailStatus.VerificationSent
                requiresRecentLogin(error) ->
                    _updateEmailStatus.value = UpdateEmailStatus.RequiresReauth
                else -> {
                    Log.w(TAG, "updateEmail falhou: $error")
                    _updateEmailStatus.value = UpdateEmailStatus.Error(error)
                }
            }
        }
    }

    fun reauthAndUpdateEmailWithPassword(
        currentPassword: String,
        newEmail: String,
        authProvider: AuthProvider
    ) {
        _updateEmailStatus.value = UpdateEmailStatus.Updating
        authProvider.reauthenticateWithPassword(currentPassword) { success, error ->
            if (success) {
                updateEmail(newEmail, authProvider)
            } else {
                Log.w(TAG, "reauthenticateWithPassword (updateEmail) falhou: $error")
                _updateEmailStatus.value = UpdateEmailStatus.Error(error)
            }
        }
    }

    fun reauthAndUpdateEmailWithGoogle(
        idToken: String?,
        newEmail: String,
        authProvider: AuthProvider
    ) {
        _updateEmailStatus.value = UpdateEmailStatus.Updating
        authProvider.reauthenticateWithGoogle(idToken) { success, error ->
            if (success) {
                updateEmail(newEmail, authProvider)
            } else {
                Log.w(TAG, "reauthenticateWithGoogle (updateEmail) falhou: $error")
                _updateEmailStatus.value = UpdateEmailStatus.Error(error)
            }
        }
    }

    fun resetUpdateEmailStatus() {
        _updateEmailStatus.value = UpdateEmailStatus.Idle
    }

    private fun requiresRecentLogin(error: String?): Boolean {
        val message = error ?: return false
        return message.contains("requires-recent-login", ignoreCase = true) ||
            message.contains("RECENT_LOGIN", ignoreCase = true)
    }

    override fun onCleared() {
        cooldownJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "AuthenticationViewModel"
        const val COOLDOWN_SECONDS = 60
    }
}
