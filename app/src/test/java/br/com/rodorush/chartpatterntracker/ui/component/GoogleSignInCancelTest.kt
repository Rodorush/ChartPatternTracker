package br.com.rodorush.chartpatterntracker.ui.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A regra decide o que vira evento no Crashlytics no login e na reautenticação com Google:
 * cancelamento fica em Log.i; o resto, em Log.e com a exceção.
 */
class GoogleSignInCancelTest {

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
}
