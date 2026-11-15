package br.com.rodorush.chartpatterntracker.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId

/**
 * Calendário de pregões da B3: quais dias têm negociação e a que horas a sessão abre.
 *
 * Fonte: calendário de negociação da B3, conferido contra a série diária da Brapi para PETR4
 * de nov/2021 a nov/2026, sem divergência, e contra a série horária do último ano para o
 * horário de abertura. A regra cobre o calendário vigente: o 20/11 só fecha a partir de 2024,
 * quando virou feriado nacional (Lei 14.759/2023). Os feriados municipais de São Paulo
 * (25/01 e 09/07) têm pregão.
 */
internal object B3Calendar {
    val ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")

    private val REGULAR_OPEN: LocalTime = LocalTime.of(10, 0)
    private val ASH_WEDNESDAY_OPEN: LocalTime = LocalTime.of(13, 0)
    private const val BLACK_CONSCIOUSNESS_SINCE = 2024

    /** Se [date] tem pregão: dia de semana fora dos feriados da B3. */
    fun isTradingDay(date: LocalDate): Boolean =
        !date.isWeekend() && date !in holidays(date.year)

    /**
     * O pregão mais recente que já tinha aberto em [now]. Antes da abertura de um dia de
     * pregão, é o pregão anterior.
     */
    fun lastOpenedSession(now: Instant): LocalDate {
        val local = now.atZone(ZONE)
        val today = local.toLocalDate()
        if (isTradingDay(today) && !local.toLocalTime().isBefore(sessionOpen(today))) return today
        // Termina: nenhum ano tem mais que alguns dias de semana seguidos sem pregão.
        return generateSequence(today.minusDays(1)) { it.minusDays(1) }.first { isTradingDay(it) }
    }

    /** Abertura da sessão em [date], no fuso da B3: 13:00 na Quarta de Cinzas, 10:00 nos demais. */
    private fun sessionOpen(date: LocalDate): LocalTime =
        if (date == easterSunday(date.year).minusDays(46)) ASH_WEDNESDAY_OPEN else REGULAR_OPEN

    private fun holidays(year: Int): Set<LocalDate> {
        val easter = easterSunday(year)
        return buildSet {
            add(LocalDate.of(year, Month.JANUARY, 1))
            add(easter.minusDays(48)) // Carnaval, segunda
            add(easter.minusDays(47)) // Carnaval, terça
            add(easter.minusDays(2)) // Sexta-feira Santa
            add(LocalDate.of(year, Month.APRIL, 21))
            add(LocalDate.of(year, Month.MAY, 1))
            add(easter.plusDays(60)) // Corpus Christi
            add(LocalDate.of(year, Month.SEPTEMBER, 7))
            add(LocalDate.of(year, Month.OCTOBER, 12))
            add(LocalDate.of(year, Month.NOVEMBER, 2))
            add(LocalDate.of(year, Month.NOVEMBER, 15))
            if (year >= BLACK_CONSCIOUSNESS_SINCE) add(LocalDate.of(year, Month.NOVEMBER, 20))
            add(LocalDate.of(year, Month.DECEMBER, 24))
            add(LocalDate.of(year, Month.DECEMBER, 25))
            add(lastWeekdayOfYear(year)) // 31/12, ou a sexta anterior se cair no fim de semana
        }
    }

    private fun lastWeekdayOfYear(year: Int): LocalDate =
        generateSequence(LocalDate.of(year, Month.DECEMBER, 31)) { it.minusDays(1) }
            .first { !it.isWeekend() }

    /** Domingo de Páscoa no calendário gregoriano, pelo algoritmo de Meeus/Jones/Butcher. */
    private fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    private fun LocalDate.isWeekend(): Boolean =
        dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
}
