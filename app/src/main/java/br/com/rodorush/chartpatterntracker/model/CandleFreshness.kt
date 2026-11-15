package br.com.rodorush.chartpatterntracker.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Decide se a série em cache ainda tem a vela mais recente que a Brapi poderia entregar,
 * para o app não chamar a `updateCandlesticks` à toa.
 *
 * A série está atualizada enquanto nenhum pregão da B3 tiver aberto depois do período da
 * última vela. Fim de semana, feriado e a madrugada antes da abertura não contam, e a vela do
 * último pregão basta. O período é a semana no `1wk`, o mês no `1mo` e o dia nos demais,
 * intradiários inclusive. Assim, durante o pregão, a série com a vela de um dia anterior está
 * desatualizada.
 */
internal object CandleFreshness {

    /**
     * @param latestCandleTime início da última vela da série, em ms epoch (a Brapi carimba a
     * vela diária às 00:00 de Brasília e a semanal e a mensal no 1º pregão do período).
     * @param now instante atual, em ms epoch.
     * @param timeframe intervalo no formato da Brapi, como em [TimeframeItem.value].
     */
    fun isUpToDate(latestCandleTime: Long, now: Long, timeframe: String): Boolean {
        val candleDay = Instant.ofEpochMilli(latestCandleTime).atZone(B3Calendar.ZONE).toLocalDate()
        val lastSession = B3Calendar.lastOpenedSession(Instant.ofEpochMilli(now))
        return !periodStart(candleDay, timeframe).isBefore(periodStart(lastSession, timeframe))
    }

    private fun periodStart(day: LocalDate, timeframe: String): LocalDate = when (timeframe) {
        "1wk" -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        "1mo" -> day.withDayOfMonth(1)
        else -> day
    }
}
