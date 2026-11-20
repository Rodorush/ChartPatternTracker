package br.com.rodorush.chartpatterntracker.viewmodel

import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider.Companion.GOOGLE_PROVIDER
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider.Companion.PASSWORD_PROVIDER
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.ReauthMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class ReauthMethodTest {

    private val both = listOf(PASSWORD_PROVIDER, GOOGLE_PROVIDER)

    @Test
    fun `senha e Google vinculados com sessao Google pede o Google`() {
        assertEquals(ReauthMethod.GOOGLE, resolveReauthMethod(GOOGLE_PROVIDER, both))
        // A ordem dos vinculados não decide.
        assertEquals(ReauthMethod.GOOGLE, resolveReauthMethod(GOOGLE_PROVIDER, both.reversed()))
    }

    @Test
    fun `senha e Google vinculados com sessao por senha pede a senha`() {
        assertEquals(ReauthMethod.PASSWORD, resolveReauthMethod(PASSWORD_PROVIDER, both))
        assertEquals(ReauthMethod.PASSWORD, resolveReauthMethod(PASSWORD_PROVIDER, both.reversed()))
    }

    @Test
    fun `senha e Google vinculados sem sessao conhecida pede a senha`() {
        assertEquals(ReauthMethod.PASSWORD, resolveReauthMethod(null, both))
        assertEquals(ReauthMethod.PASSWORD, resolveReauthMethod(null, both.reversed()))
    }

    @Test
    fun `so Google vinculado pede o Google com ou sem sessao conhecida`() {
        assertEquals(ReauthMethod.GOOGLE, resolveReauthMethod(null, listOf(GOOGLE_PROVIDER)))
        assertEquals(ReauthMethod.GOOGLE, resolveReauthMethod(GOOGLE_PROVIDER, listOf(GOOGLE_PROVIDER)))
    }

    @Test
    fun `so senha vinculada pede a senha com ou sem sessao conhecida`() {
        assertEquals(ReauthMethod.PASSWORD, resolveReauthMethod(null, listOf(PASSWORD_PROVIDER)))
        assertEquals(ReauthMethod.PASSWORD, resolveReauthMethod(PASSWORD_PROVIDER, listOf(PASSWORD_PROVIDER)))
    }

    @Test
    fun `provedor da sessao que nao esta vinculado cai nos vinculados`() {
        assertEquals(ReauthMethod.PASSWORD, resolveReauthMethod(GOOGLE_PROVIDER, listOf(PASSWORD_PROVIDER)))
        assertEquals(ReauthMethod.GOOGLE, resolveReauthMethod(PASSWORD_PROVIDER, listOf(GOOGLE_PROVIDER)))
    }

    @Test
    fun `sem provedor vinculado pede a senha`() {
        assertEquals(ReauthMethod.PASSWORD, resolveReauthMethod(null, emptyList()))
    }
}
