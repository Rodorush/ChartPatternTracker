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

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _resendCooldownSeconds = MutableStateFlow(0)
    val resendCooldownSeconds = _resendCooldownSeconds.asStateFlow()

    private val _verificationStatus = MutableStateFlow<VerificationStatus>(VerificationStatus.Idle)
    val verificationStatus = _verificationStatus.asStateFlow()

    private val _isCheckingVerification = MutableStateFlow(false)
    val isCheckingVerification = _isCheckingVerification.asStateFlow()

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

    override fun onCleared() {
        cooldownJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "AuthenticationViewModel"
        const val COOLDOWN_SECONDS = 60
    }
}
