package br.com.rodorush.chartpatterntracker.activity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherReentryTest {

    @Test
    fun `icone com a tarefa ja aberta fecha a instancia nova`() {
        assertTrue(isRedundantLauncherStart(isTaskRoot = false, isLauncherIntent = true))
    }

    @Test
    fun `icone abrindo a tarefa segue o fluxo normal`() {
        assertFalse(isRedundantLauncherStart(isTaskRoot = true, isLauncherIntent = true))
    }

    @Test
    fun `abertura explicita fora da raiz segue o fluxo normal`() {
        // Ex.: o Sair abre a AuthenticationActivity com intent explícito; não pode fechar sozinha.
        assertFalse(isRedundantLauncherStart(isTaskRoot = false, isLauncherIntent = false))
    }

    @Test
    fun `abertura explicita como raiz segue o fluxo normal`() {
        assertFalse(isRedundantLauncherStart(isTaskRoot = true, isLauncherIntent = false))
    }
}
