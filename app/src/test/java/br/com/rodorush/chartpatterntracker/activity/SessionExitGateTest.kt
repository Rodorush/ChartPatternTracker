package br.com.rodorush.chartpatterntracker.activity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExitGateTest {

    @Test
    fun `sessao ativa nao pede saida`() {
        val gate = SessionExitGate()

        assertFalse(gate.onAuthStateChanged(isSignedIn = true))
        // A sessão ativa não consome o portão: o Sair continua disponível.
        assertTrue(gate.tryBeginExit())
    }

    @Test
    fun `saida em curso so depois do pedido`() {
        // O Voltar da MainActivity consulta isExiting: durante a saída, não manda o app para trás.
        val gate = SessionExitGate()

        assertFalse(gate.isExiting)
        gate.onAuthStateChanged(isSignedIn = true)
        assertFalse(gate.isExiting)
        gate.tryBeginExit()
        assertTrue(gate.isExiting)
    }

    @Test
    fun `sessao encerrada marca a saida em curso`() {
        val gate = SessionExitGate()

        gate.onAuthStateChanged(isSignedIn = false)
        assertTrue(gate.isExiting)
    }

    @Test
    fun `sessao encerrada pede saida uma unica vez`() {
        val gate = SessionExitGate()

        assertTrue(gate.onAuthStateChanged(isSignedIn = false))
        assertFalse(gate.onAuthStateChanged(isSignedIn = false))
    }

    @Test
    fun `sair seguido do aviso do listener da uma saida so`() {
        val gate = SessionExitGate()

        assertTrue(gate.tryBeginExit())
        assertFalse(gate.onAuthStateChanged(isSignedIn = false))
    }

    @Test
    fun `sessao encerrada seguida do sair da uma saida so`() {
        // Exclusão de conta: o delete encerra a sessão antes de o Perfil chamar o Sair.
        val gate = SessionExitGate()

        assertTrue(gate.onAuthStateChanged(isSignedIn = false))
        assertFalse(gate.tryBeginExit())
    }
}
