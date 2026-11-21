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
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val cooldownTickMillis: Long = 1000L,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    sealed class VerificationStatus {
        data object Idle : VerificationStatus()
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

    /** Como o Perfil pede a reautenticação de uma operação sensível (ver [resolveReauthMethod]). */
    enum class ReauthMethod { PASSWORD, GOOGLE }

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _resendCooldownSeconds = MutableStateFlow(0)
    val resendCooldownSeconds = _resendCooldownSeconds.asStateFlow()

    private val _verificationStatus = MutableStateFlow<VerificationStatus>(VerificationStatus.Idle)
    val verificationStatus = _verificationStatus.asStateFlow()

    private val _isCheckingVerification = MutableStateFlow(false)
    val isCheckingVerification = _isCheckingVerification.asStateFlow()

    private val _isSendingVerification = MutableStateFlow(false)

    /**
     * `true` enquanto o envio do e-mail de verificação espera o Firebase. Fica fora de
     * [verificationStatus] porque o [checkVerified] grava naquele status e liberaria a trava do
     * envio no meio do caminho.
     */
    val isSendingVerification = _isSendingVerification.asStateFlow()

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

    private val _sessionProvider = MutableStateFlow<String?>(null)

    /**
     * Provedor pelo qual a sessão atual foi aberta, carregado por [loadSessionProvider] e relido
     * antes de cada pedido de reautenticação. Fica `null` até a primeira leitura com sucesso; uma
     * leitura que falha mantém o valor anterior.
     */
    val sessionProvider = _sessionProvider.asStateFlow()

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

    /**
     * Reenvia o e-mail de verificação; o sucesso arma o cooldown, que bloqueia o reenvio seguinte.
     * Com um envio já em andamento, a chamada é ignorada: um segundo toque no Reenviar antes de o
     * Firebase responder mandaria o e-mail duas vezes. O andamento fica em [isSendingVerification].
     */
    fun sendVerificationEmail(authProvider: AuthProvider) {
        if (_resendCooldownSeconds.value > 0) return
        if (!_isSendingVerification.tryBegin(true)) return ignored("sendVerificationEmail")
        authProvider.sendEmailVerification { success, error ->
            if (success) {
                _verificationStatus.value = VerificationStatus.Sent
                armResendCooldown()
            } else {
                Log.w(TAG, "sendVerificationEmail falhou: $error")
                _verificationStatus.value = VerificationStatus.SendError(error)
            }
            // Solta a trava só depois de armar o cooldown, para não abrir uma brecha entre os dois.
            _isSendingVerification.value = false
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

    /**
     * Envia ao [email] o link de redefinição de senha; o sucesso arma o cooldown, que bloqueia o
     * envio seguinte. Com um envio já em andamento, a chamada é ignorada: um segundo toque antes de
     * o Firebase responder mandaria o e-mail duas vezes.
     */
    fun sendPasswordReset(email: String, authProvider: AuthProvider) {
        if (_resendCooldownSeconds.value > 0) return
        if (!_resetPasswordStatus.tryBegin(ResetPasswordStatus.Sending)) return ignored("sendPasswordReset")
        authProvider.sendPasswordResetEmail(email) { success, error ->
            if (success) {
                // Arma o cooldown antes de o Sent soltar a trava, como no sendVerificationEmail.
                armResendCooldown()
                _resetPasswordStatus.value = ResetPasswordStatus.Sent
            } else {
                Log.w(TAG, "sendPasswordReset falhou: $error")
                _resetPasswordStatus.value = ResetPasswordStatus.Error(error)
            }
        }
    }

    fun resetPasswordResetStatus() {
        _resetPasswordStatus.value = ResetPasswordStatus.Idle
    }

    /**
     * Exclui a conta logada. Com uma exclusão já em andamento, por esta entrada ou pelas de
     * reautenticação, a chamada é ignorada: um segundo toque no Excluir antes de o diálogo sumir
     * chamaria o Firebase de novo.
     */
    fun deleteAccount(authProvider: AuthProvider) {
        if (!_deleteAccountStatus.tryBegin(DeleteAccountStatus.Deleting)) return ignored("deleteAccount")
        runDeleteAccount(authProvider)
    }

    // Corpo da exclusão, sem a trava: a continuação da reautenticação chega aqui com o status já em
    // Deleting.
    private fun runDeleteAccount(authProvider: AuthProvider) {
        authProvider.deleteCurrentUser { success, error ->
            when {
                success -> _deleteAccountStatus.value = DeleteAccountStatus.Deleted
                requiresRecentLogin(error) -> readSessionProvider(authProvider) {
                    _deleteAccountStatus.value = DeleteAccountStatus.RequiresReauth
                }
                else -> {
                    Log.w(TAG, "deleteAccount falhou: $error")
                    _deleteAccountStatus.value = DeleteAccountStatus.Error(error)
                }
            }
        }
    }

    /** Reautentica pela senha e exclui a conta; ignorada como [deleteAccount]. */
    fun reauthenticateAndDeleteWithPassword(password: String, authProvider: AuthProvider) {
        if (!_deleteAccountStatus.tryBegin(DeleteAccountStatus.Deleting)) {
            return ignored("reauthenticateAndDeleteWithPassword")
        }
        authProvider.reauthenticateWithPassword(password) { success, error ->
            if (success) {
                runDeleteAccount(authProvider)
            } else {
                Log.w(TAG, "reauthenticateWithPassword falhou: $error")
                _deleteAccountStatus.value = DeleteAccountStatus.Error(error)
            }
        }
    }

    /** Reautentica pelo Google e exclui a conta; ignorada como [deleteAccount]. */
    fun reauthenticateAndDeleteWithGoogle(idToken: String?, authProvider: AuthProvider) {
        if (!_deleteAccountStatus.tryBegin(DeleteAccountStatus.Deleting)) {
            return ignored("reauthenticateAndDeleteWithGoogle")
        }
        authProvider.reauthenticateWithGoogle(idToken) { success, error ->
            if (success) {
                runDeleteAccount(authProvider)
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

    /**
     * Troca a senha da conta logada. Com uma troca já em andamento, por esta entrada ou pela de
     * reautenticação, a chamada é ignorada: um segundo toque no Salvar antes de o diálogo sumir
     * chamaria o Firebase de novo.
     */
    fun updatePassword(newPassword: String, authProvider: AuthProvider) {
        if (!_updatePasswordStatus.tryBegin(UpdatePasswordStatus.Updating)) return ignored("updatePassword")
        runUpdatePassword(newPassword, authProvider)
    }

    // Corpo da troca, sem a trava, pelo mesmo motivo de runDeleteAccount.
    private fun runUpdatePassword(newPassword: String, authProvider: AuthProvider) {
        authProvider.updatePassword(newPassword) { success, error ->
            when {
                success -> _updatePasswordStatus.value = UpdatePasswordStatus.Updated
                requiresRecentLogin(error) -> readSessionProvider(authProvider) {
                    _updatePasswordStatus.value = UpdatePasswordStatus.RequiresReauth
                }
                else -> {
                    Log.w(TAG, "updatePassword falhou: $error")
                    _updatePasswordStatus.value = UpdatePasswordStatus.Error(error)
                }
            }
        }
    }

    /** Reautentica pela senha atual e troca a senha; ignorada como [updatePassword]. */
    fun reauthAndUpdatePasswordWithPassword(
        currentPassword: String,
        newPassword: String,
        authProvider: AuthProvider
    ) {
        if (!_updatePasswordStatus.tryBegin(UpdatePasswordStatus.Updating)) {
            return ignored("reauthAndUpdatePasswordWithPassword")
        }
        authProvider.reauthenticateWithPassword(currentPassword) { success, error ->
            if (success) {
                runUpdatePassword(newPassword, authProvider)
            } else {
                Log.w(TAG, "reauthenticateWithPassword (updatePassword) falhou: $error")
                _updatePasswordStatus.value = UpdatePasswordStatus.Error(error)
            }
        }
    }

    fun resetUpdatePasswordStatus() {
        _updatePasswordStatus.value = UpdatePasswordStatus.Idle
    }

    // --- NF-03c: alterar e-mail via verifyBeforeUpdateEmail (exige sessão recente) ---

    /**
     * Envia ao [newEmail] o link que confirma a troca de e-mail. Com uma troca já em andamento, por
     * esta entrada ou pelas de reautenticação, a chamada é ignorada: um segundo toque no Salvar antes
     * de o diálogo sumir enviaria o link duas vezes.
     */
    fun updateEmail(newEmail: String, authProvider: AuthProvider) {
        if (!_updateEmailStatus.tryBegin(UpdateEmailStatus.Updating)) return ignored("updateEmail")
        runUpdateEmail(newEmail, authProvider)
    }

    // Corpo da troca, sem a trava, pelo mesmo motivo de runDeleteAccount.
    private fun runUpdateEmail(newEmail: String, authProvider: AuthProvider) {
        authProvider.verifyBeforeUpdateEmail(newEmail) { success, error ->
            when {
                success -> _updateEmailStatus.value = UpdateEmailStatus.VerificationSent
                requiresRecentLogin(error) -> readSessionProvider(authProvider) {
                    _updateEmailStatus.value = UpdateEmailStatus.RequiresReauth
                }
                else -> {
                    Log.w(TAG, "updateEmail falhou: $error")
                    _updateEmailStatus.value = UpdateEmailStatus.Error(error)
                }
            }
        }
    }

    /** Reautentica pela senha e envia o link da troca de e-mail; ignorada como [updateEmail]. */
    fun reauthAndUpdateEmailWithPassword(
        currentPassword: String,
        newEmail: String,
        authProvider: AuthProvider
    ) {
        if (!_updateEmailStatus.tryBegin(UpdateEmailStatus.Updating)) {
            return ignored("reauthAndUpdateEmailWithPassword")
        }
        authProvider.reauthenticateWithPassword(currentPassword) { success, error ->
            if (success) {
                runUpdateEmail(newEmail, authProvider)
            } else {
                Log.w(TAG, "reauthenticateWithPassword (updateEmail) falhou: $error")
                _updateEmailStatus.value = UpdateEmailStatus.Error(error)
            }
        }
    }

    /** Reautentica pelo Google e envia o link da troca de e-mail; ignorada como [updateEmail]. */
    fun reauthAndUpdateEmailWithGoogle(
        idToken: String?,
        newEmail: String,
        authProvider: AuthProvider
    ) {
        if (!_updateEmailStatus.tryBegin(UpdateEmailStatus.Updating)) {
            return ignored("reauthAndUpdateEmailWithGoogle")
        }
        authProvider.reauthenticateWithGoogle(idToken) { success, error ->
            if (success) {
                runUpdateEmail(newEmail, authProvider)
            } else {
                Log.w(TAG, "reauthenticateWithGoogle (updateEmail) falhou: $error")
                _updateEmailStatus.value = UpdateEmailStatus.Error(error)
            }
        }
    }

    fun resetUpdateEmailStatus() {
        _updateEmailStatus.value = UpdateEmailStatus.Idle
    }

    private fun requiresRecentLogin(error: String?): Boolean =
        error == AuthProvider.REQUIRES_RECENT_LOGIN

    /**
     * Marca a operação como em andamento ([inProgress]) e devolve `true`; devolve `false` se ela
     * já estava, para a chamada repetida ser ignorada. A troca é atômica: a trava não depende de os
     * toques e os callbacks do Firebase rodarem na mesma thread.
     */
    private fun <T> MutableStateFlow<T>.tryBegin(inProgress: T): Boolean =
        getAndUpdate { inProgress } != inProgress

    private fun ignored(operation: String) {
        Log.i(TAG, "$operation ignorada: a mesma operação já está em andamento")
    }

    /** Lê o provedor da sessão atual para [sessionProvider]. */
    fun loadSessionProvider(authProvider: AuthProvider) = readSessionProvider(authProvider) {}

    /**
     * Lê o provedor da sessão e então chama [then]. Antes de pedir reautenticação, a releitura
     * acompanha uma reautenticação anterior, que muda o provedor da sessão, e uma leitura que
     * falhou na abertura da tela. Durante ela, o status da operação segue em andamento.
     */
    private fun readSessionProvider(authProvider: AuthProvider, then: () -> Unit) {
        authProvider.currentSignInProvider { provider ->
            Log.d(TAG, "Provedor da sessão: $provider")
            if (provider != null) _sessionProvider.value = provider
            then()
        }
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

/**
 * Decide como pedir a reautenticação. Vale o provedor com que o usuário abriu a sessão, desde que
 * ainda esteja vinculado: uma conta com senha e Google pode ter entrado por qualquer um dos dois.
 * Sem esse provedor (leitura pendente ou falha), a senha tem preferência quando está vinculada.
 *
 * @param signInProvider o provedor da sessão ([AuthProvider.currentSignInProvider]), ou `null`
 *   quando não é conhecido.
 * @param linkedProviders os provedores vinculados ([AuthProvider.currentUserProviderIds]).
 */
internal fun resolveReauthMethod(
    signInProvider: String?,
    linkedProviders: List<String>
): AuthenticationViewModel.ReauthMethod {
    val hasPassword = AuthProvider.PASSWORD_PROVIDER in linkedProviders
    val hasGoogle = AuthProvider.GOOGLE_PROVIDER in linkedProviders
    return when {
        signInProvider == AuthProvider.GOOGLE_PROVIDER && hasGoogle ->
            AuthenticationViewModel.ReauthMethod.GOOGLE
        signInProvider == AuthProvider.PASSWORD_PROVIDER && hasPassword ->
            AuthenticationViewModel.ReauthMethod.PASSWORD
        hasPassword -> AuthenticationViewModel.ReauthMethod.PASSWORD
        hasGoogle -> AuthenticationViewModel.ReauthMethod.GOOGLE
        else -> AuthenticationViewModel.ReauthMethod.PASSWORD
    }
}
