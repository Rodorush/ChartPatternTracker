package br.com.rodorush.chartpatterntracker.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Excluir a conta e alterar o e-mail só seguem sem a credencial do aparelho quando ele não tem
 * bloqueio de tela. Com a tela aberta, quem segue é o retorno dela; se ela não abre, nada segue.
 */
class DeviceCredentialTest {

    @Test
    fun `aparelho sem bloqueio segue sem abrir a tela`() {
        val events = mutableListOf<String>()

        val failure = requestDeviceCredential<String>(
            intent = null,
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
            intent = "intent",
            launch = { events += "launch:$it" },
            proceed = { events += "proceed" }
        )

        assertNull(failure)
        assertEquals(listOf("launch:intent"), events)
    }

    @Test
    fun `tela que nao abre devolve a falha e nao segue`() {
        // Seguir mesmo assim faria a operação sem a credencial num aparelho que tem bloqueio.
        val cause = IllegalStateException("tela de credencial ausente")
        var proceeded = false

        val failure = requestDeviceCredential(
            intent = "intent",
            launch = { throw cause },
            proceed = { proceeded = true }
        )

        assertSame(cause, failure)
        assertFalse(proceeded)
    }
}
