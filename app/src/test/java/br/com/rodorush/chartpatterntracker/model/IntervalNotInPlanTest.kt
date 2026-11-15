package br.com.rodorush.chartpatterntracker.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O código vai pelo nome, como em `FirebaseFunctionsException.Code.name`: o enum não carrega
 * na JVM do teste unitário (ver [isIntervalNotInPlan]).
 */
class IntervalNotInPlanTest {

    private val planDetails = mapOf("reason" to PLAN_INTERVAL_REASON)

    @Test
    fun `failed precondition com o reason do plano e recusa por plano`() {
        assertTrue(isIntervalNotInPlan("FAILED_PRECONDITION", planDetails))
    }

    @Test
    fun `failed precondition sem o reason do plano nao e recusa por plano`() {
        assertFalse(isIntervalNotInPlan("FAILED_PRECONDITION", null))
        assertFalse(isIntervalNotInPlan("FAILED_PRECONDITION", emptyMap<String, Any>()))
        assertFalse(isIntervalNotInPlan("FAILED_PRECONDITION", mapOf("reason" to "other")))
    }

    @Test
    fun `outros codigos com o reason do plano nao sao recusa por plano`() {
        for (code in listOf("INTERNAL", "RESOURCE_EXHAUSTED", "INVALID_ARGUMENT", "UNAUTHENTICATED")) {
            assertFalse(code, isIntervalNotInPlan(code, planDetails))
        }
    }

    @Test
    fun `details fora do formato de mapa nao e recusa por plano`() {
        assertFalse(isIntervalNotInPlan("FAILED_PRECONDITION", PLAN_INTERVAL_REASON))
        assertFalse(isIntervalNotInPlan("FAILED_PRECONDITION", listOf(PLAN_INTERVAL_REASON)))
    }
}
