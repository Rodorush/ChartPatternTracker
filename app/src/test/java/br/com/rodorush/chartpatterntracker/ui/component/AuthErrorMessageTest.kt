package br.com.rodorush.chartpatterntracker.ui.component

import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * O erro da autenticação chega à tela como código e sai como texto do strings.xml, nos três
 * locales. O código sem texto próprio cai no texto genérico de cada operação.
 */
class AuthErrorMessageTest {

    @Test
    fun `codigos com texto proprio`() {
        val fallback = R.string.login_error

        assertEquals(R.string.network_error, authErrorMessage(AuthProvider.NETWORK, fallback))
        assertEquals(
            R.string.auth_error_too_many_requests,
            authErrorMessage(AuthProvider.TOO_MANY_REQUESTS, fallback)
        )
        assertEquals(
            R.string.auth_error_user_mismatch,
            authErrorMessage(AuthProvider.USER_MISMATCH, fallback)
        )
        assertEquals(
            R.string.email_already_exists,
            authErrorMessage(AuthProvider.EMAIL_IN_USE, fallback)
        )
    }

    @Test
    fun `credencial recusada usa e-mail e senha por padrao`() {
        assertEquals(
            R.string.invalid_email_password,
            authErrorMessage(AuthProvider.INVALID_CREDENTIAL, R.string.login_error)
        )
    }

    @Test
    fun `credencial recusada usa o texto que a tela passa`() {
        // A reautenticação do Perfil só pede a senha.
        assertEquals(
            R.string.auth_error_wrong_password,
            authErrorMessage(
                AuthProvider.INVALID_CREDENTIAL,
                fallback = R.string.auth_error_generic,
                invalidCredential = R.string.auth_error_wrong_password
            )
        )
    }

    @Test
    fun `codigo sem texto proprio cai no texto generico da operacao`() {
        listOf(
            null,
            AuthProvider.MISSING_ID_TOKEN,
            AuthProvider.NO_CURRENT_USER,
            AuthProvider.REQUIRES_RECENT_LOGIN,
            "codigo-desconhecido"
        ).forEach { code ->
            assertEquals(R.string.registration_error, authErrorMessage(code, R.string.registration_error))
            assertEquals(R.string.auth_error_generic, authErrorMessage(code, R.string.auth_error_generic))
        }
    }
}
