package br.com.rodorush.chartpatterntracker.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisclaimerGateTest {

    @Test
    fun `conta sem aceite no aparelho ve o aviso`() {
        assertTrue(needsDisclaimer(acceptedUids = emptySet(), currentUid = "conta-a"))
    }

    @Test
    fun `mesma conta que volta nao ve o aviso de novo`() {
        assertFalse(needsDisclaimer(acceptedUids = setOf("conta-a"), currentUid = "conta-a"))
    }

    @Test
    fun `conta nova no aparelho ve o aviso`() {
        // Também a conta recriada depois de uma exclusão, que ganha outro uid.
        assertTrue(needsDisclaimer(acceptedUids = setOf("conta-a"), currentUid = "conta-b"))
    }

    @Test
    fun `conta que volta depois de outra usar o aparelho nao ve o aviso de novo`() {
        assertFalse(needsDisclaimer(acceptedUids = setOf("conta-a", "conta-b"), currentUid = "conta-a"))
    }

    @Test
    fun `sem sessao o aceite e pedido`() {
        assertTrue(needsDisclaimer(acceptedUids = setOf("conta-a"), currentUid = null))
    }
}
