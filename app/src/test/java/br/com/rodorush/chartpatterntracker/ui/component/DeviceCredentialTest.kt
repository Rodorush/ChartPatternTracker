package br.com.rodorush.chartpatterntracker.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Excluir a conta e alterar o e-mail só seguem sem a credencial do aparelho quando ele não tem
 * bloqueio de tela. Com a tela aberta, quem segue é o retorno dela; se ela não abre, nada segue.
 */
class DeviceCredentialTest {

    @Test
    fun `aparelho sem bloqueio segue sem abrir a tela`() {
        val events = mutableListOf<String>()

        val failure = requestDeviceCredential(
            deviceSecure = false,
            createIntent = { events += "createIntent"; "intent" },
            launch = { events += "launch" },
            proceed = { events += "proceed" }
        )

        assertNull(failure)
        assertEquals(listOf("proceed"), events)
    }

    @Test
    fun `aparelho com bloqueio abre a tela e nao segue antes do retorno dela`() {
        val events = mutableListOf<String>()

        val failure = requestDeviceCredential(
            deviceSecure = true,
            createIntent = { "intent" },
            launch = { events += "launch:$it" },
            proceed = { events += "proceed" }
        )

        assertNull(failure)
        assertEquals(listOf("launch:intent"), events)
    }

    @Test
    fun `aparelho com bloqueio sem intent devolve a falha e nao segue`() {
        // Tratar o intent nulo como aparelho sem bloqueio faria a operação sem a credencial.
        var launched = false
        var proceeded = false

        val failure = requestDeviceCredential<String>(
            deviceSecure = true,
            createIntent = { null },
            launch = { launched = true },
            proceed = { proceeded = true }
        )

        assertTrue(failure is IllegalStateException)
        assertFalse(launched)
        assertFalse(proceeded)
    }

    @Test
    fun `tela que nao abre devolve a falha e nao segue`() {
        // Seguir mesmo assim faria a operação sem a credencial num aparelho que tem bloqueio.
        val cause = IllegalStateException("tela de credencial ausente")
        var proceeded = false

        val failure = requestDeviceCredential(
            deviceSecure = true,
            createIntent = { "intent" },
            launch = { throw cause },
            proceed = { proceeded = true }
        )

        assertSame(cause, failure)
        assertFalse(proceeded)
    }
}
