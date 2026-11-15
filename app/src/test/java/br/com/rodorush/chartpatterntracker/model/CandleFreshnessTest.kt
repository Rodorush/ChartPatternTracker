package br.com.rodorush.chartpatterntracker.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

/**
 * As velas de base são as últimas que a Brapi devolveu para PETR4 no domingo 15/11/2026,
 * com o último pregão na sexta 13/11.
 */
class CandleFreshnessTest {

    private fun brt(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, B3Calendar.ZONE).toInstant().toEpochMilli()

    private val lastDaily = brt(2026, 11, 13)
    private val lastHourly = brt(2026, 11, 13, 16, 0)
    private val lastWeekly = brt(2026, 11, 9)
    private val lastMonthly = brt(2026, 11, 3)

    private fun assertAllUpToDate(now: Long) {
        assertTrue(CandleFreshness.isUpToDate(lastDaily, now, "1d"))
        assertTrue(CandleFreshness.isUpToDate(lastHourly, now, "1h"))
        assertTrue(CandleFreshness.isUpToDate(lastWeekly, now, "1wk"))
        assertTrue(CandleFreshness.isUpToDate(lastMonthly, now, "1mo"))
    }

    @Test
    fun `sabado com a vela do ultimo pregao esta atualizado`() {
        assertAllUpToDate(brt(2026, 11, 14, 12, 0))
    }

    @Test
    fun `domingo com a vela do ultimo pregao esta atualizado`() {
        assertAllUpToDate(brt(2026, 11, 15, 20, 0))
    }

    @Test
    fun `segunda antes da abertura ainda esta atualizado`() {
        assertAllUpToDate(brt(2026, 11, 16, 9, 59))
    }

    @Test
    fun `segunda na abertura desatualiza dia e semana mas nao o mes`() {
        val now = brt(2026, 11, 16, 10, 0)
        assertFalse(CandleFreshness.isUpToDate(lastDaily, now, "1d"))
        assertFalse(CandleFreshness.isUpToDate(lastHourly, now, "1h"))
        assertFalse(CandleFreshness.isUpToDate(lastWeekly, now, "1wk"))
        assertTrue(CandleFreshness.isUpToDate(lastMonthly, now, "1mo"))
    }

    @Test
    fun `vela da vespera de feriado vale ate o proximo pregao abrir`() {
        val thursday = brt(2026, 11, 19)
        assertTrue(CandleFreshness.isUpToDate(thursday, brt(2026, 11, 20, 12, 0), "1d"))
        assertTrue(CandleFreshness.isUpToDate(thursday, brt(2026, 11, 21, 12, 0), "1d"))
        assertTrue(CandleFreshness.isUpToDate(thursday, brt(2026, 11, 22, 12, 0), "1d"))
        assertTrue(CandleFreshness.isUpToDate(thursday, brt(2026, 11, 23, 9, 59), "1d"))
        assertFalse(CandleFreshness.isUpToDate(thursday, brt(2026, 11, 23, 10, 0), "1d"))
    }

    @Test
    fun `feriado na segunda mantem a vela de sexta`() {
        val friday = brt(2026, 10, 30)
        assertTrue(CandleFreshness.isUpToDate(friday, brt(2026, 11, 2, 12, 0), "1d"))
        assertFalse(CandleFreshness.isUpToDate(friday, brt(2026, 11, 3, 10, 0), "1d"))
    }

    @Test
    fun `semana que comeca na terca conta como a mesma semana`() {
        val tuesday = brt(2026, 11, 3)
        assertTrue(CandleFreshness.isUpToDate(tuesday, brt(2026, 11, 5, 15, 0), "1wk"))
        assertTrue(CandleFreshness.isUpToDate(tuesday, brt(2026, 11, 8, 12, 0), "1wk"))
        assertFalse(CandleFreshness.isUpToDate(tuesday, brt(2026, 11, 9, 10, 0), "1wk"))
    }

    @Test
    fun `carnaval mantem a vela de sexta ate a quarta de cinzas abrir`() {
        val friday = brt(2026, 2, 13)
        assertTrue(CandleFreshness.isUpToDate(friday, brt(2026, 2, 17, 12, 0), "1d"))
        assertTrue(CandleFreshness.isUpToDate(friday, brt(2026, 2, 18, 12, 59), "1d"))
        assertFalse(CandleFreshness.isUpToDate(friday, brt(2026, 2, 18, 13, 0), "1d"))
    }

    @Test
    fun `vela mensal vale ate o primeiro pregao do mes seguinte abrir`() {
        assertTrue(CandleFreshness.isUpToDate(lastMonthly, brt(2026, 11, 30, 15, 0), "1mo"))
        assertTrue(CandleFreshness.isUpToDate(lastMonthly, brt(2026, 12, 1, 9, 59), "1mo"))
        assertFalse(CandleFreshness.isUpToDate(lastMonthly, brt(2026, 12, 1, 10, 0), "1mo"))
    }

    @Test
    fun `durante o pregao so a vela do dia esta atualizada`() {
        val now = brt(2026, 11, 17, 15, 0)
        assertFalse(CandleFreshness.isUpToDate(brt(2026, 11, 16), now, "1d"))
        assertTrue(CandleFreshness.isUpToDate(brt(2026, 11, 17), now, "1d"))
    }

    @Test
    fun `serie sem vela esta desatualizada`() {
        assertFalse(CandleFreshness.isUpToDate(0, brt(2026, 11, 15, 12, 0), "1d"))
    }

    @Test
    fun `tempo grafico desconhecido usa o dia`() {
        assertTrue(CandleFreshness.isUpToDate(lastHourly, brt(2026, 11, 15, 12, 0), "4h"))
        assertFalse(CandleFreshness.isUpToDate(lastHourly, brt(2026, 11, 16, 10, 0), "4h"))
    }
}
