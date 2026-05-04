package br.com.rodorush.chartpatterntracker

import br.com.rodorush.chartpatterntracker.util.provider.mock.MockAuthProvider
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel
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
}
