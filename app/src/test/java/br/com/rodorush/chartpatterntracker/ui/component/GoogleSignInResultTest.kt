package br.com.rodorush.chartpatterntracker.ui.component

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A triagem decide o que vira evento no Crashlytics no login e na reautenticação com Google:
 * só a falha real vai para Log.e com a exceção; cancelamento e login já em andamento, não.
 */
class GoogleSignInResultTest {

    @Test
    fun `seletor fechado com 12501 e cancelamento`() {
        assertTrue(isGoogleSignInCancelled(hasIntent = true, statusCode = 12501))
    }

    @Test
    fun `retorno sem intent e cancelamento qualquer que seja o status`() {
        // Sem intent o Google não diz o motivo, e o statusCode da exceção não serve de triagem.
        assertTrue(isGoogleSignInCancelled(hasIntent = false, statusCode = 8))
        assertTrue(isGoogleSignInCancelled(hasIntent = false, statusCode = 12501))
    }

    @Test
    fun `falha de configuracao com intent nao e cancelamento`() {
        // DEVELOPER_ERROR (10): SHA-1 não registrado ou client ID errado.
        assertFalse(isGoogleSignInCancelled(hasIntent = true, statusCode = 10))
    }

    @Test
    fun `falha de rede com intent nao e cancelamento`() {
        // NETWORK_ERROR (7) segue em Log.e, por decisão do mantenedor no ot5bIDRA.
        assertFalse(isGoogleSignInCancelled(hasIntent = true, statusCode = 7))
    }

    @Test
    fun `12502 com intent vira InProgress e nao falha`() {
        // SIGN_IN_CURRENTLY_IN_PROGRESS: toque duplo no botão do Google.
        val outcome = readGoogleSignInResult(apiFailure(12502), hasIntent = true)

        assertEquals(GoogleSignInOutcome.InProgress, outcome)
    }

    @Test
    fun `12502 sem intent vira cancelamento`() {
        // Sem intent o status não serve de triagem, como na regra do cancelamento.
        val outcome = readGoogleSignInResult(apiFailure(12502), hasIntent = false)

        assertEquals(GoogleSignInOutcome.Cancelled(12502), outcome)
    }

    @Test
    fun `12501 com intent vira Cancelled`() {
        val outcome = readGoogleSignInResult(apiFailure(12501), hasIntent = true)

        assertEquals(GoogleSignInOutcome.Cancelled(12501), outcome)
    }

    @Test
    fun `falha de configuracao vira Failure com o status`() {
        val outcome = readGoogleSignInResult(apiFailure(10), hasIntent = true)

        assertTrue(outcome is GoogleSignInOutcome.Failure)
        assertEquals(10, (outcome as GoogleSignInOutcome.Failure).statusCode)
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
    fun `task concluida vira Account`() {
        // A conta real não se constrói fora do Android; a task sem conta cobre o caminho do
        // sucesso, que chega ao Account com idToken nulo.
        val task = Tasks.forResult<GoogleSignInAccount>(null)

        val outcome = readGoogleSignInResult(task, hasIntent = true)

        assertEquals(GoogleSignInOutcome.Account(idToken = null), outcome)
    }

    private fun apiFailure(statusCode: Int) =
        Tasks.forException<GoogleSignInAccount>(ApiException(Status(statusCode)))
}
