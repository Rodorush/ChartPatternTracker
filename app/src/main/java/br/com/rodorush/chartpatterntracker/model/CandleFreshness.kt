package br.com.rodorush.chartpatterntracker.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Decide se a série em cache ainda vale, para o app não chamar a `updateCandlesticks` à toa
 * nem seguir, depois do fechamento, com a vela que leu pela metade. Durante o pregão, a vela
 * em formação vale como foi lida.
 *
 * A série está atualizada quando cumpre as duas condições:
 * - Nenhum pregão da B3 abriu depois do período da última vela. Fim de semana, feriado e a
 *   madrugada antes da abertura não contam, e a vela do último pregão basta. O período é a
 *   semana no `1wk`, o mês no `1mo` e o dia nos demais, intradiários inclusive. Assim, durante
 *   o pregão, a série com a vela de um dia anterior está desatualizada.
 * - Ela foi lida da Brapi depois do último fechamento ([B3Calendar.lastSettlement]). A Brapi
 *   devolve a vela do período em andamento, e a leitura feita com o pregão aberto traz a
 *   última vela parcial. Depois do fechamento, a série é buscada mais uma vez. Vale também
 *   para o `1wk` e o `1mo`, cuja vela em formação muda a cada pregão.
 */
internal object CandleFreshness {

    /**
     * @param latestCandleTime início da última vela da série, em ms epoch (a Brapi carimba a
     * vela diária às 00:00 de Brasília e a semanal e a mensal no 1º pregão do período).
     * @param fetchedAt instante em que a série foi lida da Brapi, em ms epoch. `0` quando não
     * se sabe, o que dá a série como desatualizada.
     * @param now instante atual, em ms epoch.
     * @param timeframe intervalo no formato da Brapi, como em [TimeframeItem.value].
     */
    fun isUpToDate(latestCandleTime: Long, fetchedAt: Long, now: Long, timeframe: String): Boolean {
        val nowInstant = Instant.ofEpochMilli(now)
        if (fetchedAt < B3Calendar.lastSettlement(nowInstant).toEpochMilli()) return false
        val candleDay = Instant.ofEpochMilli(latestCandleTime).atZone(B3Calendar.ZONE).toLocalDate()
        val lastSession = B3Calendar.lastOpenedSession(nowInstant)
        return !periodStart(candleDay, timeframe).isBefore(periodStart(lastSession, timeframe))
    }

    private fun periodStart(day: LocalDate, timeframe: String): LocalDate = when (timeframe) {
        "1wk" -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        "1mo" -> day.withDayOfMonth(1)
        else -> day
    }
}
