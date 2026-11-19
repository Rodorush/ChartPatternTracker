package br.com.rodorush.chartpatterntracker.ui.component

import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A reautenticação com Google só prova que o dono está presente se o seletor de contas
 * aparecer: o signOut vem antes do lançamento, e sem ele concluído o seletor não abre.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoogleAccountChooserTest {

    @Test
    fun `signOut concluido abre o seletor depois dele`() = runTest {
        val events = mutableListOf<String>()

        val failure = launchGoogleAccountChooser(
            signOut = { events += "signOut"; Tasks.forResult(null) },
            launch = { events += "launch" }
        )

        assertNull(failure)
        assertEquals(listOf("signOut", "launch"), events)
    }

    @Test
    fun `signOut com falha nao abre o seletor e devolve a falha`() = runTest {
        // Abrir mesmo assim devolveria a conta conectada sem o seletor.
        val cause = IllegalStateException("signOut falhou")
        var launched = false

        val failure = launchGoogleAccountChooser(
            signOut = { Tasks.forException(cause) },
            launch = { launched = true }
        )

        assertSame(cause, failure)
        assertFalse(launched)
    }

    @Test
    fun `task do signOut cancelada e falha, nao cancelamento da coroutine`() = runTest {
        var launched = false

        val failure = launchGoogleAccountChooser(
            signOut = { Tasks.forCanceled() },
            launch = { launched = true }
        )

        assertTrue(failure is CancellationException)
        assertFalse(launched)
    }

    @Test
    fun `coroutine cancelada durante o signOut nao abre o seletor`() = runTest {
        // A tela saiu da composição enquanto o signOut corria: o launcher dela já não vale.
        val signOut = TaskCompletionSource<Void>()
        var launched = false
        val job = launch {
            launchGoogleAccountChooser(signOut = { signOut.task }, launch = { launched = true })
        }
        runCurrent()

        job.cancel()
        signOut.setResult(null)
        job.join()

        assertTrue(job.isCancelled)
        assertFalse(launched)
    }
}
