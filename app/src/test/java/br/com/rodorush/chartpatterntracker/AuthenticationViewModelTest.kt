package br.com.rodorush.chartpatterntracker

import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.mock.MockAuthProvider
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.DeleteAccountStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.ResetPasswordStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.UpdateEmailStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.UpdateNameStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.UpdatePasswordStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticationViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(): AuthenticationViewModel =
        AuthenticationViewModel(cooldownTickMillis = 1000L, ioDispatcher = dispatcher)

    @Test
    fun `sendVerificationEmail success arms cooldown and emits Sent`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com")
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendVerificationEmail(provider)

        assertEquals(VerificationStatus.Sent, vm.verificationStatus.value)
        assertEquals(60, vm.resendCooldownSeconds.value)
        assertEquals(1, provider.sendEmailVerificationCalls)
    }

    @Test
    fun `sendVerificationEmail failure emits SendError and does not arm cooldown`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setSendVerificationSucceeds(false)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendVerificationEmail(provider)

        assertTrue(vm.verificationStatus.value is VerificationStatus.SendError)
        assertEquals(0, vm.resendCooldownSeconds.value)
    }

    @Test
    fun `sendVerificationEmail is no-op while cooldown active`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com")
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendVerificationEmail(provider)
        val callsAfterFirst = provider.sendEmailVerificationCalls

        vm.sendVerificationEmail(provider)

        assertEquals(callsAfterFirst, provider.sendEmailVerificationCalls)
    }

    @Test
    fun `armResendCooldown decrements down to zero`() = runTest(dispatcher) {
        val vm = newViewModel()
        advanceUntilIdle()

        vm.armResendCooldown(seconds = 3)
        assertEquals(3, vm.resendCooldownSeconds.value)

        advanceTimeBy(1100)
        assertEquals(2, vm.resendCooldownSeconds.value)

        advanceTimeBy(2000)
        assertEquals(0, vm.resendCooldownSeconds.value)
    }

    @Test
    fun `checkVerified emits Verified when reload flips isEmailVerified`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setReloadFlipsToVerified(true)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.checkVerified(provider)

        assertEquals(VerificationStatus.Verified, vm.verificationStatus.value)
    }

    @Test
    fun `checkVerified emits NotYetVerified when reload keeps unverified`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setReloadFlipsToVerified(false)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.checkVerified(provider)

        assertEquals(VerificationStatus.NotYetVerified, vm.verificationStatus.value)
    }

    @Test
    fun `checkVerified reload failure emits CheckError with the code, not SendError`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setReloadFails(AuthProvider.NETWORK)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.checkVerified(provider)

        assertEquals(VerificationStatus.CheckError(AuthProvider.NETWORK), vm.verificationStatus.value)
        assertFalse(vm.isCheckingVerification.value)
    }

    @Test
    fun `checkVerified failure does not touch the send lock nor the cooldown`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setReloadFails(null)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.checkVerified(provider)

        assertEquals(VerificationStatus.CheckError(null), vm.verificationStatus.value)
        assertFalse(vm.isSendingVerification.value)
        assertEquals(0, vm.resendCooldownSeconds.value)
    }

    @Test
    fun `sendPasswordReset success arms cooldown and emits Sent`() = runTest(dispatcher) {
        val provider = MockAuthProvider()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendPasswordReset("user@example.com", provider)

        assertEquals(ResetPasswordStatus.Sent, vm.resetPasswordStatus.value)
        assertEquals(60, vm.resendCooldownSeconds.value)
        assertEquals(1, provider.sendPasswordResetEmailCalls)
        assertEquals("user@example.com", provider.lastPasswordResetEmail)
    }

    @Test
    fun `sendPasswordReset failure emits Error and does not arm cooldown`() = runTest(dispatcher) {
        val provider = MockAuthProvider().apply {
            setSendPasswordResetSucceeds(false)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendPasswordReset("user@example.com", provider)

        assertTrue(vm.resetPasswordStatus.value is ResetPasswordStatus.Error)
        assertEquals(0, vm.resendCooldownSeconds.value)
    }

    @Test
    fun `sendPasswordReset is no-op while cooldown active`() = runTest(dispatcher) {
        val provider = MockAuthProvider()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendPasswordReset("user@example.com", provider)
        val callsAfterFirst = provider.sendPasswordResetEmailCalls

        vm.sendPasswordReset("user@example.com", provider)

        assertEquals(callsAfterFirst, provider.sendPasswordResetEmailCalls)
    }

    @Test
    fun `deleteAccount success emits Deleted`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com")
        val vm = newViewModel()
        advanceUntilIdle()

        vm.deleteAccount(provider)

        assertEquals(DeleteAccountStatus.Deleted, vm.deleteAccountStatus.value)
        assertEquals(1, provider.deleteCalls)
    }

    @Test
    fun `deleteAccount with requires-recent-login emits RequiresReauth`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setDeleteRequiresRecentLogin(true)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.deleteAccount(provider)

        assertEquals(DeleteAccountStatus.RequiresReauth, vm.deleteAccountStatus.value)
    }

    @Test
    fun `reauthenticate with password then delete succeeds`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setDeleteRequiresRecentLogin(true)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.deleteAccount(provider)
        assertEquals(DeleteAccountStatus.RequiresReauth, vm.deleteAccountStatus.value)

        vm.reauthenticateAndDeleteWithPassword("password", provider)

        assertEquals(DeleteAccountStatus.Deleted, vm.deleteAccountStatus.value)
        assertEquals(1, provider.reauthCalls)
        assertEquals(2, provider.deleteCalls)
    }

    @Test
    fun `reauthenticate failure emits Error`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setReauthSucceeds(false)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.reauthenticateAndDeleteWithPassword("wrong", provider)

        assertTrue(vm.deleteAccountStatus.value is DeleteAccountStatus.Error)
    }

    @Test
    fun `resetDeleteAccountStatus returns to Idle`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com")
        val vm = newViewModel()
        advanceUntilIdle()

        vm.deleteAccount(provider)
        vm.resetDeleteAccountStatus()

        assertEquals(DeleteAccountStatus.Idle, vm.deleteAccountStatus.value)
    }

    // --- NF-03d: alterar nome ---

    @Test
    fun `updateDisplayName success emits Updated`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com")
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateDisplayName("Rodolfo", provider)

        assertEquals(UpdateNameStatus.Updated, vm.updateNameStatus.value)
        assertEquals(1, provider.updateDisplayNameCalls)
    }

    @Test
    fun `updateDisplayName failure emits Error`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setUpdateNameSucceeds(false)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateDisplayName("Rodolfo", provider)

        assertTrue(vm.updateNameStatus.value is UpdateNameStatus.Error)
    }

    // --- NF-03b: alterar senha ---

    @Test
    fun `updatePassword success emits Updated`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com")
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updatePassword("newPassword", provider)

        assertEquals(UpdatePasswordStatus.Updated, vm.updatePasswordStatus.value)
        assertEquals(1, provider.updatePasswordCalls)
    }

    @Test
    fun `updatePassword with requires-recent-login emits RequiresReauth`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setUpdatePasswordRequiresRecentLogin(true)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updatePassword("newPassword", provider)

        assertEquals(UpdatePasswordStatus.RequiresReauth, vm.updatePasswordStatus.value)
    }

    @Test
    fun `reauthenticate with password then updatePassword succeeds`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setUpdatePasswordRequiresRecentLogin(true)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updatePassword("newPassword", provider)
        assertEquals(UpdatePasswordStatus.RequiresReauth, vm.updatePasswordStatus.value)

        vm.reauthAndUpdatePasswordWithPassword("password", "newPassword", provider)

        assertEquals(UpdatePasswordStatus.Updated, vm.updatePasswordStatus.value)
        assertEquals(1, provider.reauthCalls)
        assertEquals(2, provider.updatePasswordCalls)
    }

    @Test
    fun `reauthenticate failure on updatePassword emits Error`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setReauthSucceeds(false)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.reauthAndUpdatePasswordWithPassword("wrong", "newPassword", provider)

        assertTrue(vm.updatePasswordStatus.value is UpdatePasswordStatus.Error)
    }

    // --- NF-03c: alterar e-mail ---

    @Test
    fun `updateEmail success emits VerificationSent`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com")
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateEmail("new@example.com", provider)

        assertEquals(UpdateEmailStatus.VerificationSent, vm.updateEmailStatus.value)
        assertEquals(1, provider.verifyBeforeUpdateEmailCalls)
        assertEquals("new@example.com", provider.lastNewEmail)
    }

    @Test
    fun `updateEmail with requires-recent-login emits RequiresReauth`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setUpdateEmailRequiresRecentLogin(true)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateEmail("new@example.com", provider)

        assertEquals(UpdateEmailStatus.RequiresReauth, vm.updateEmailStatus.value)
    }

    @Test
    fun `reauthenticate with password then updateEmail succeeds`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setUpdateEmailRequiresRecentLogin(true)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateEmail("new@example.com", provider)
        assertEquals(UpdateEmailStatus.RequiresReauth, vm.updateEmailStatus.value)

        vm.reauthAndUpdateEmailWithPassword("password", "new@example.com", provider)

        assertEquals(UpdateEmailStatus.VerificationSent, vm.updateEmailStatus.value)
        assertEquals(1, provider.reauthCalls)
        assertEquals(2, provider.verifyBeforeUpdateEmailCalls)
    }

    @Test
    fun `reauthenticate failure on updateEmail emits Error`() = runTest(dispatcher) {
        val provider = MockAuthProvider(email = "user@example.com").apply {
            setReauthSucceeds(false)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.reauthAndUpdateEmailWithPassword("wrong", "new@example.com", provider)

        assertTrue(vm.updateEmailStatus.value is UpdateEmailStatus.Error)
    }

    @Test
    fun `loadSessionProvider exposes the provider of the current session`() = runTest(dispatcher) {
        val provider = MockAuthProvider(
            email = "user@example.com",
            providerIds = listOf(AuthProvider.PASSWORD_PROVIDER, AuthProvider.GOOGLE_PROVIDER),
            signInProvider = AuthProvider.GOOGLE_PROVIDER
        )
        val vm = newViewModel()
        advanceUntilIdle()
        assertNull(vm.sessionProvider.value)

        vm.loadSessionProvider(provider)

        assertEquals(AuthProvider.GOOGLE_PROVIDER, vm.sessionProvider.value)
    }

    @Test
    fun `requires-recent-login re-reads the session provider before RequiresReauth`() = runTest(dispatcher) {
        val provider = MockAuthProvider(
            email = "user@example.com",
            providerIds = listOf(AuthProvider.PASSWORD_PROVIDER, AuthProvider.GOOGLE_PROVIDER),
            signInProvider = AuthProvider.GOOGLE_PROVIDER
        ).apply {
            setUpdateEmailRequiresRecentLogin(true)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateEmail("new@example.com", provider)

        assertEquals(UpdateEmailStatus.RequiresReauth, vm.updateEmailStatus.value)
        assertEquals(AuthProvider.GOOGLE_PROVIDER, vm.sessionProvider.value)
    }

    @Test
    fun `a failed session provider read keeps the previous value`() = runTest(dispatcher) {
        val provider = MockAuthProvider(
            email = "user@example.com",
            providerIds = listOf(AuthProvider.PASSWORD_PROVIDER, AuthProvider.GOOGLE_PROVIDER),
            signInProvider = AuthProvider.GOOGLE_PROVIDER
        ).apply {
            setDeleteRequiresRecentLogin(true)
        }
        val vm = newViewModel()
        advanceUntilIdle()
        vm.loadSessionProvider(provider)

        provider.setSignInProviderForTest(null)
        vm.deleteAccount(provider)

        assertEquals(DeleteAccountStatus.RequiresReauth, vm.deleteAccountStatus.value)
        assertEquals(AuthProvider.GOOGLE_PROVIDER, vm.sessionProvider.value)
    }

    @Test
    fun `updatePassword with requires-recent-login re-reads the session provider`() = runTest(dispatcher) {
        val provider = MockAuthProvider(
            email = "user@example.com",
            providerIds = listOf(AuthProvider.PASSWORD_PROVIDER, AuthProvider.GOOGLE_PROVIDER),
            signInProvider = AuthProvider.GOOGLE_PROVIDER
        ).apply {
            setUpdatePasswordRequiresRecentLogin(true)
        }
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updatePassword("newpass123", provider)

        assertEquals(UpdatePasswordStatus.RequiresReauth, vm.updatePasswordStatus.value)
        assertEquals(AuthProvider.GOOGLE_PROVIDER, vm.sessionProvider.value)
    }

    // --- Chamada repetida com a operação em andamento (segundo toque antes de o diálogo sumir) ---

    @Test
    fun `deleteAccount repeated while the first is pending calls Firebase once`() = runTest(dispatcher) {
        val provider = HeldAuthProvider()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.deleteAccount(provider)
        vm.deleteAccount(provider)

        assertEquals(1, provider.deleteCallbacks.size)
        assertEquals(DeleteAccountStatus.Deleting, vm.deleteAccountStatus.value)

        provider.deleteCallbacks.single()(true, null)

        assertEquals(DeleteAccountStatus.Deleted, vm.deleteAccountStatus.value)
    }

    @Test
    fun `deleteAccount after a failed attempt calls Firebase again`() = runTest(dispatcher) {
        val provider = HeldAuthProvider()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.deleteAccount(provider)
        provider.deleteCallbacks.single()(false, null)
        assertTrue(vm.deleteAccountStatus.value is DeleteAccountStatus.Error)

        vm.deleteAccount(provider)

        assertEquals(2, provider.deleteCallbacks.size)
        assertEquals(DeleteAccountStatus.Deleting, vm.deleteAccountStatus.value)
    }

    @Test
    fun `reauthenticate and delete repeated while pending reauthenticates once and deletes once`() =
        runTest(dispatcher) {
            val provider = HeldAuthProvider()
            val vm = newViewModel()
            advanceUntilIdle()

            vm.reauthenticateAndDeleteWithPassword("password", provider)
            vm.reauthenticateAndDeleteWithPassword("password", provider)
            vm.deleteAccount(provider)

            assertEquals(1, provider.reauthCallbacks.size)
            assertTrue(provider.deleteCallbacks.isEmpty())

            // A continuação da reautenticação não passa pela trava.
            provider.reauthCallbacks.single()(true, null)
            assertEquals(1, provider.deleteCallbacks.size)

            provider.deleteCallbacks.single()(true, null)
            assertEquals(DeleteAccountStatus.Deleted, vm.deleteAccountStatus.value)
        }

    @Test
    fun `reauthenticate with Google and delete is ignored while a delete is pending`() = runTest(dispatcher) {
        val mock = MockAuthProvider(email = "user@example.com")
        val provider = HeldAuthProvider(mock)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.deleteAccount(provider)
        vm.reauthenticateAndDeleteWithGoogle("id-token", provider)

        assertEquals(0, mock.reauthCalls)
        assertEquals(1, provider.deleteCallbacks.size)
        // A chamada ignorada não mexe no status, que mantém o Perfil ocupado.
        assertEquals(DeleteAccountStatus.Deleting, vm.deleteAccountStatus.value)

        provider.deleteCallbacks.single()(true, null)

        assertEquals(DeleteAccountStatus.Deleted, vm.deleteAccountStatus.value)
    }

    @Test
    fun `updateEmail repeated while the first is pending sends the link once`() = runTest(dispatcher) {
        val provider = HeldAuthProvider()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updateEmail("new@example.com", provider)
        vm.updateEmail("new@example.com", provider)

        assertEquals(1, provider.emailCallbacks.size)
        assertEquals(UpdateEmailStatus.Updating, vm.updateEmailStatus.value)

        provider.emailCallbacks.single()(true, null)

        assertEquals(UpdateEmailStatus.VerificationSent, vm.updateEmailStatus.value)
    }

    @Test
    fun `reauthenticate and updateEmail repeated while pending reauthenticates once and sends once`() =
        runTest(dispatcher) {
            val mock = MockAuthProvider(email = "user@example.com")
            val provider = HeldAuthProvider(mock)
            val vm = newViewModel()
            advanceUntilIdle()

            vm.reauthAndUpdateEmailWithPassword("password", "new@example.com", provider)
            vm.reauthAndUpdateEmailWithPassword("password", "new@example.com", provider)
            vm.reauthAndUpdateEmailWithGoogle("id-token", "new@example.com", provider)

            assertEquals(1, provider.reauthCallbacks.size)
            assertEquals(0, mock.reauthCalls)

            provider.reauthCallbacks.single()(true, null)
            assertEquals(1, provider.emailCallbacks.size)

            provider.emailCallbacks.single()(true, null)
            assertEquals(UpdateEmailStatus.VerificationSent, vm.updateEmailStatus.value)
        }

    @Test
    fun `updatePassword repeated while the first is pending calls Firebase once`() = runTest(dispatcher) {
        val provider = HeldAuthProvider()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.updatePassword("newpass123", provider)
        vm.updatePassword("newpass123", provider)

        assertEquals(1, provider.passwordCallbacks.size)
        assertEquals(UpdatePasswordStatus.Updating, vm.updatePasswordStatus.value)

        provider.passwordCallbacks.single()(true, null)

        assertEquals(UpdatePasswordStatus.Updated, vm.updatePasswordStatus.value)
    }

    @Test
    fun `reauthenticate and updatePassword repeated while pending reauthenticates once and updates once`() =
        runTest(dispatcher) {
            val provider = HeldAuthProvider()
            val vm = newViewModel()
            advanceUntilIdle()

            vm.reauthAndUpdatePasswordWithPassword("current", "newpass123", provider)
            vm.reauthAndUpdatePasswordWithPassword("current", "newpass123", provider)
            vm.updatePassword("newpass123", provider)

            assertEquals(1, provider.reauthCallbacks.size)
            assertTrue(provider.passwordCallbacks.isEmpty())

            provider.reauthCallbacks.single()(true, null)
            assertEquals(1, provider.passwordCallbacks.size)

            provider.passwordCallbacks.single()(true, null)
            assertEquals(UpdatePasswordStatus.Updated, vm.updatePasswordStatus.value)
        }

    @Test
    fun `sendPasswordReset repeated while the first is pending calls Firebase once`() = runTest(dispatcher) {
        val provider = HeldAuthProvider()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendPasswordReset("user@example.com", provider)
        vm.sendPasswordReset("user@example.com", provider)

        assertEquals(1, provider.resetCallbacks.size)
        assertEquals(ResetPasswordStatus.Sending, vm.resetPasswordStatus.value)

        provider.resetCallbacks.single()(true, null)

        assertEquals(ResetPasswordStatus.Sent, vm.resetPasswordStatus.value)
        assertEquals(60, vm.resendCooldownSeconds.value)
    }

    @Test
    fun `sendPasswordReset after a failed attempt calls Firebase again`() = runTest(dispatcher) {
        val provider = HeldAuthProvider()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendPasswordReset("user@example.com", provider)
        provider.resetCallbacks.single()(false, null)
        assertTrue(vm.resetPasswordStatus.value is ResetPasswordStatus.Error)

        vm.sendPasswordReset("user@example.com", provider)

        assertEquals(2, provider.resetCallbacks.size)
        assertEquals(ResetPasswordStatus.Sending, vm.resetPasswordStatus.value)
    }

    @Test
    fun `sendVerificationEmail repeated while the first is pending sends once`() = runTest(dispatcher) {
        val provider = HeldAuthProvider()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendVerificationEmail(provider)
        vm.sendVerificationEmail(provider)

        assertEquals(1, provider.verificationCallbacks.size)
        assertTrue(vm.isSendingVerification.value)

        provider.verificationCallbacks.single()(true, null)

        assertEquals(VerificationStatus.Sent, vm.verificationStatus.value)
        assertEquals(60, vm.resendCooldownSeconds.value)
        assertFalse(vm.isSendingVerification.value)
    }

    @Test
    fun `checkVerified during a pending send does not release the send lock`() = runTest(dispatcher) {
        val mock = MockAuthProvider(email = "user@example.com").apply {
            setReloadFlipsToVerified(false)
        }
        val provider = HeldAuthProvider(mock)
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendVerificationEmail(provider)
        // Volta do app de e-mail (ON_RESUME) antes de o Firebase responder; a tela zera o status.
        vm.checkVerified(provider)
        assertEquals(VerificationStatus.NotYetVerified, vm.verificationStatus.value)
        vm.resetVerificationStatus()

        vm.sendVerificationEmail(provider)

        assertEquals(1, provider.verificationCallbacks.size)
        assertTrue(vm.isSendingVerification.value)
    }

    @Test
    fun `sendVerificationEmail after a failed attempt sends again`() = runTest(dispatcher) {
        val provider = HeldAuthProvider()
        val vm = newViewModel()
        advanceUntilIdle()

        vm.sendVerificationEmail(provider)
        provider.verificationCallbacks.single()(false, null)
        assertTrue(vm.verificationStatus.value is VerificationStatus.SendError)
        assertFalse(vm.isSendingVerification.value)

        vm.sendVerificationEmail(provider)

        assertEquals(2, provider.verificationCallbacks.size)
        assertTrue(vm.isSendingVerification.value)
    }
}

/**
 * Guarda o callback da exclusão, das trocas de e-mail e de senha, da reautenticação por senha e
 * dos envios da redefinição de senha e da verificação de e-mail, sem responder, para o teste ver a
 * operação em andamento; o [MockAuthProvider] responde na hora. O resto, inclusive a reautenticação
 * pelo Google, vem do [delegate].
 */
private class HeldAuthProvider(
    delegate: AuthProvider = MockAuthProvider(email = "user@example.com")
) : AuthProvider by delegate {
    val deleteCallbacks = mutableListOf<(Boolean, String?) -> Unit>()
    val emailCallbacks = mutableListOf<(Boolean, String?) -> Unit>()
    val passwordCallbacks = mutableListOf<(Boolean, String?) -> Unit>()
    val reauthCallbacks = mutableListOf<(Boolean, String?) -> Unit>()
    val resetCallbacks = mutableListOf<(Boolean, String?) -> Unit>()
    val verificationCallbacks = mutableListOf<(Boolean, String?) -> Unit>()

    override fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        resetCallbacks += onResult
    }

    override fun sendEmailVerification(onResult: (Boolean, String?) -> Unit) {
        verificationCallbacks += onResult
    }

    override fun deleteCurrentUser(onResult: (Boolean, String?) -> Unit) {
        deleteCallbacks += onResult
    }

    override fun verifyBeforeUpdateEmail(newEmail: String, onResult: (Boolean, String?) -> Unit) {
        emailCallbacks += onResult
    }

    override fun updatePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        passwordCallbacks += onResult
    }

    override fun reauthenticateWithPassword(password: String, onResult: (Boolean, String?) -> Unit) {
        reauthCallbacks += onResult
    }
}
