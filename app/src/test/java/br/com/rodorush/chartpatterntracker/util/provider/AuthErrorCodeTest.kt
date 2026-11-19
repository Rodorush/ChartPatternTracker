package br.com.rodorush.chartpatterntracker.util.provider

import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A tela mostra o erro da autenticação pelo código, nunca pela mensagem do Firebase, que é
 * texto livre em inglês. Aqui se testa a triagem pelo `errorCode`; o despacho por tipo de
 * [authErrorCode] fica fora, porque as exceções do Firebase não se constroem no teste local
 * (o construtor chama `TextUtils.isEmpty`, que o android.jar de teste não implementa).
 */
class AuthErrorCodeTest {

    @Test
    fun `conta diferente da sessao vira USER_MISMATCH`() {
        assertEquals(AuthProvider.USER_MISMATCH, firebaseAuthErrorCode("ERROR_USER_MISMATCH"))
    }

    @Test
    fun `senha errada vira INVALID_CREDENTIAL`() {
        assertEquals(AuthProvider.INVALID_CREDENTIAL, firebaseAuthErrorCode("ERROR_INVALID_CREDENTIAL"))
        assertEquals(AuthProvider.INVALID_CREDENTIAL, firebaseAuthErrorCode("ERROR_WRONG_PASSWORD"))
    }

    @Test
    fun `usuario inexistente ou desativado vira INVALID_CREDENTIAL`() {
        // Mesmo texto da senha errada, para não revelar se o e-mail tem conta.
        assertEquals(AuthProvider.INVALID_CREDENTIAL, firebaseAuthErrorCode("ERROR_USER_NOT_FOUND"))
        assertEquals(AuthProvider.INVALID_CREDENTIAL, firebaseAuthErrorCode("ERROR_USER_DISABLED"))
    }

    @Test
    fun `email ja usado vira EMAIL_IN_USE`() {
        assertEquals(AuthProvider.EMAIL_IN_USE, firebaseAuthErrorCode("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals(
            AuthProvider.EMAIL_IN_USE,
            firebaseAuthErrorCode("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL")
        )
    }

    @Test
    fun `senha fraca e email malformado nao viram INVALID_CREDENTIAL`() {
        // Chegam na mesma classe da senha errada; no Perfil, INVALID_CREDENTIAL diria
        // "senha incorreta" para uma senha nova fraca ou um e-mail novo malformado.
        assertNull(firebaseAuthErrorCode("ERROR_WEAK_PASSWORD"))
        assertNull(firebaseAuthErrorCode("ERROR_INVALID_EMAIL"))
    }

    @Test
    fun `errorCode sem traducao vira null`() {
        assertNull(firebaseAuthErrorCode("ERROR_INTERNAL_ERROR"))
        assertNull(firebaseAuthErrorCode(""))
    }

    @Test
    fun `falha que nao e do Firebase vira null`() {
        assertNull(authErrorCode(IllegalStateException("qualquer")))
        assertNull(authErrorCode(null))
    }
}
