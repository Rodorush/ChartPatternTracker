package br.com.rodorush.chartpatterntracker.ui.component

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A triagem decide o que vira evento no Crashlytics no login e na reautenticação com Google:
 * interrupção esperada fica em Log.i; o resto, em Log.e com a exceção.
 */
class GoogleSignInResultTest {

    @Test
    fun `seletor fechado com 12501 e cancelamento`() {
        assertTrue(isExpectedGoogleSignInFailure(hasIntent = true, statusCode = 12501))
    }

    @Test
    fun `retorno sem intent e cancelamento qualquer que seja o status`() {
        // Sem intent o Google não diz o motivo, e o statusCode da exceção não serve de triagem.
        assertTrue(isExpectedGoogleSignInFailure(hasIntent = false, statusCode = 8))
        assertTrue(isExpectedGoogleSignInFailure(hasIntent = false, statusCode = 12501))
    }

    @Test
    fun `login ja em andamento com 12502 nao e erro`() {
        // SIGN_IN_CURRENTLY_IN_PROGRESS: toque duplo no botão do Google.
        assertTrue(isExpectedGoogleSignInFailure(hasIntent = true, statusCode = 12502))
    }

    @Test
    fun `falha de configuracao com intent nao e cancelamento`() {
        // DEVELOPER_ERROR (10): SHA-1 não registrado ou client ID errado.
        assertFalse(isExpectedGoogleSignInFailure(hasIntent = true, statusCode = 10))
    }

    @Test
    fun `falha de rede com intent nao e cancelamento`() {
        // NETWORK_ERROR (7) segue em Log.e, por decisão do mantenedor no ot5bIDRA.
        assertFalse(isExpectedGoogleSignInFailure(hasIntent = true, statusCode = 7))
    }

    @Test
    fun `falha que nao e ApiException vira Failure sem lancar`() {
        // getResult(ApiException::class.java) embrulha essa falha em RuntimeExecutionException,
        // que antes escapava do catch e fechava o app.
        val cause = IllegalStateException("falha fora da ApiException")
        val task = Tasks.forException<GoogleSignInAccount>(cause)

        val outcome = readGoogleSignInResult(task, hasIntent = true)

        assertTrue(outcome is GoogleSignInOutcome.Failure)
        val failure = outcome as GoogleSignInOutcome.Failure
        assertTrue(failure.error is RuntimeExecutionException)
        assertEquals(cause, failure.error.cause)
        assertNull(failure.statusCode)
    }

    @Test
    fun `conta sem idToken vira Account com idToken nulo`() {
        val task = Tasks.forResult<GoogleSignInAccount>(null)

        val outcome = readGoogleSignInResult(task, hasIntent = true)

        assertEquals(GoogleSignInOutcome.Account(idToken = null), outcome)
    }
}
