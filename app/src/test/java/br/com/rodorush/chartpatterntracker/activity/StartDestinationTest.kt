package br.com.rodorush.chartpatterntracker.activity

import org.junit.Assert.assertEquals
import org.junit.Test

class StartDestinationTest {

    @Test
    fun `sem usuario pede login`() {
        assertEquals(
            StartDestination.LOGIN,
            resolveStartDestination(isSignedIn = false, isPasswordProvider = false, isEmailVerified = false)
        )
    }

    @Test
    fun `conta de senha sem verificacao vai para a verificacao de e-mail`() {
        assertEquals(
            StartDestination.VERIFY_EMAIL,
            resolveStartDestination(isSignedIn = true, isPasswordProvider = true, isEmailVerified = false)
        )
    }

    @Test
    fun `conta de senha verificada entra no menu`() {
        assertEquals(
            StartDestination.MAIN,
            resolveStartDestination(isSignedIn = true, isPasswordProvider = true, isEmailVerified = true)
        )
    }

    @Test
    fun `conta Google entra no menu independentemente da verificacao`() {
        assertEquals(
            StartDestination.MAIN,
            resolveStartDestination(isSignedIn = true, isPasswordProvider = false, isEmailVerified = false)
        )
        assertEquals(
            StartDestination.MAIN,
            resolveStartDestination(isSignedIn = true, isPasswordProvider = false, isEmailVerified = true)
        )
    }
}
