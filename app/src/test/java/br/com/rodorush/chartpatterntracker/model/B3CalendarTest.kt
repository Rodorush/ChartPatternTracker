package br.com.rodorush.chartpatterntracker.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/** Datas conferidas contra a série diária e a horária da Brapi para PETR4. */
class B3CalendarTest {

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)

    private fun brt(y: Int, m: Int, d: Int, h: Int, min: Int): Instant =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, B3Calendar.ZONE).toInstant()

    @Test
    fun `fim de semana nao tem pregao`() {
        assertFalse(B3Calendar.isTradingDay(day(2026, 11, 14)))
        assertFalse(B3Calendar.isTradingDay(day(2026, 11, 15)))
        assertTrue(B3Calendar.isTradingDay(day(2026, 11, 13)))
    }

    @Test
    fun `sexta-feira santa segue a pascoa de cada ano`() {
        assertFalse(B3Calendar.isTradingDay(day(2025, 4, 18)))
        assertFalse(B3Calendar.isTradingDay(day(2026, 4, 3)))
        assertFalse(B3Calendar.isTradingDay(day(2027, 3, 26)))
        assertTrue(B3Calendar.isTradingDay(day(2026, 4, 2)))
    }

    @Test
    fun `feriados moveis de 2026 fecham a bolsa`() {
        assertFalse(B3Calendar.isTradingDay(day(2026, 2, 16)))
        assertFalse(B3Calendar.isTradingDay(day(2026, 2, 17)))
        assertTrue(B3Calendar.isTradingDay(day(2026, 2, 18)))
        assertFalse(B3Calendar.isTradingDay(day(2026, 6, 4)))
    }

    @Test
    fun `feriados fixos fecham a bolsa`() {
        assertFalse(B3Calendar.isTradingDay(day(2026, 1, 1)))
        assertFalse(B3Calendar.isTradingDay(day(2026, 4, 21)))
        assertFalse(B3Calendar.isTradingDay(day(2026, 5, 1)))
        assertFalse(B3Calendar.isTradingDay(day(2026, 9, 7)))
        assertFalse(B3Calendar.isTradingDay(day(2026, 10, 12)))
        assertFalse(B3Calendar.isTradingDay(day(2026, 11, 2)))
        assertFalse(B3Calendar.isTradingDay(day(2024, 11, 15)))
        assertFalse(B3Calendar.isTradingDay(day(2025, 12, 24)))
        assertFalse(B3Calendar.isTradingDay(day(2025, 12, 25)))
    }

    @Test
    fun `20 de novembro so fecha a partir de 2024`() {
        assertTrue(B3Calendar.isTradingDay(day(2023, 11, 20)))
        assertFalse(B3Calendar.isTradingDay(day(2025, 11, 20)))
    }

    @Test
    fun `feriados municipais de sao paulo tem pregao`() {
        assertTrue(B3Calendar.isTradingDay(day(2024, 1, 25)))
        assertTrue(B3Calendar.isTradingDay(day(2026, 7, 9)))
    }

    @Test
    fun `ultimo dia util do ano fecha mesmo quando 31 de dezembro cai no fim de semana`() {
        assertFalse(B3Calendar.isTradingDay(day(2024, 12, 31)))
        assertTrue(B3Calendar.isTradingDay(day(2024, 12, 30)))
        assertFalse(B3Calendar.isTradingDay(day(2022, 12, 30)))
        assertFalse(B3Calendar.isTradingDay(day(2023, 12, 29)))
    }

    @Test
    fun `antes da abertura o ultimo pregao e o anterior`() {
        assertEquals(day(2026, 11, 13), B3Calendar.lastOpenedSession(brt(2026, 11, 16, 9, 59)))
        assertEquals(day(2026, 11, 16), B3Calendar.lastOpenedSession(brt(2026, 11, 16, 10, 0)))
    }

    @Test
    fun `quarta de cinzas so abre as 13h`() {
        assertEquals(day(2026, 2, 13), B3Calendar.lastOpenedSession(brt(2026, 2, 18, 12, 59)))
        assertEquals(day(2026, 2, 18), B3Calendar.lastOpenedSession(brt(2026, 2, 18, 13, 0)))
    }

    @Test
    fun `pregao do dia so fecha as 19h`() {
        assertEquals(brt(2026, 11, 13, 19, 0), B3Calendar.lastSettlement(brt(2026, 11, 16, 18, 59)))
        assertEquals(brt(2026, 11, 16, 19, 0), B3Calendar.lastSettlement(brt(2026, 11, 16, 19, 0)))
        assertEquals(brt(2026, 11, 16, 19, 0), B3Calendar.lastSettlement(brt(2026, 11, 17, 9, 0)))
    }

    @Test
    fun `fim de semana e feriado mantem o fechamento do ultimo pregao`() {
        assertEquals(brt(2026, 11, 13, 19, 0), B3Calendar.lastSettlement(brt(2026, 11, 14, 20, 0)))
        assertEquals(brt(2026, 11, 13, 19, 0), B3Calendar.lastSettlement(brt(2026, 11, 15, 20, 0)))
        assertEquals(brt(2026, 11, 19, 19, 0), B3Calendar.lastSettlement(brt(2026, 11, 20, 20, 0)))
        assertEquals(brt(2026, 11, 19, 19, 0), B3Calendar.lastSettlement(brt(2026, 11, 23, 18, 59)))
    }

    @Test
    fun `quarta de cinzas fecha no horario normal`() {
        assertEquals(brt(2026, 2, 13, 19, 0), B3Calendar.lastSettlement(brt(2026, 2, 18, 18, 59)))
        assertEquals(brt(2026, 2, 18, 19, 0), B3Calendar.lastSettlement(brt(2026, 2, 18, 19, 0)))
    }
}
