package br.com.rodorush.chartpatterntracker.activity

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherReentryTest {

    private val launcherCategories = setOf(Intent.CATEGORY_LAUNCHER)

    // Flags que o launcher e o monkey mandam: NEW_TASK | RESET_TASK_IF_NEEDED.
    private val launcherFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED

    @Test
    fun `icone com a tarefa ja aberta fecha a instancia nova`() {
        assertTrue(
            isRedundantLauncherStart(
                isTaskRoot = false,
                action = Intent.ACTION_MAIN,
                categories = launcherCategories,
                flags = launcherFlags
            )
        )
    }

    @Test
    fun `icone abrindo a tarefa segue o fluxo normal`() {
        assertFalse(
            isRedundantLauncherStart(
                isTaskRoot = true,
                action = Intent.ACTION_MAIN,
                categories = launcherCategories,
                flags = launcherFlags
            )
        )
    }

    @Test
    fun `Sair abre o login com intent explicito e nao fecha sozinho`() {
        assertFalse(
            isRedundantLauncherStart(
                isTaskRoot = false,
                action = null,
                categories = null,
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
    }

    @Test
    fun `intent do icone sem NEW_TASK cai na tarefa de quem abriu e nao fecha`() {
        assertFalse(
            isRedundantLauncherStart(
                isTaskRoot = false,
                action = Intent.ACTION_MAIN,
                categories = launcherCategories,
                flags = 0
            )
        )
    }

    @Test
    fun `MAIN sem a categoria LAUNCHER nao e o icone`() {
        assertFalse(
            isRedundantLauncherStart(
                isTaskRoot = false,
                action = Intent.ACTION_MAIN,
                categories = null,
                flags = launcherFlags
            )
        )
    }

    @Test
    fun `categoria LAUNCHER com outra acao nao e o icone`() {
        assertFalse(
            isRedundantLauncherStart(
                isTaskRoot = false,
                action = Intent.ACTION_VIEW,
                categories = launcherCategories,
                flags = launcherFlags
            )
        )
    }
}
