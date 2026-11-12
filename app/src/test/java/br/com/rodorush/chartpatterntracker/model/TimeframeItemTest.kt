package br.com.rodorush.chartpatterntracker.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeframeItemTest {

    /** Valores do parâmetro `interval` documentados em https://brapi.dev/docs/acoes. */
    private val brapiIntervals = setOf(
        "1m", "2m", "5m", "15m", "30m", "60m", "90m", "1h", "1d", "5d", "1wk", "1mo", "3mo"
    )

    @Test
    fun todoTempoGraficoEUmIntervaloQueABrapiConhece() {
        val unknown = TimeframeItem.availableTimeframes.map { it.value }.filterNot { it in brapiIntervals }
        assertTrue("Intervalos que a Brapi recusa com 400: $unknown", unknown.isEmpty())
    }

    @Test
    fun listaDeTemposGraficosNaoRepeteValor() {
        val values = TimeframeItem.availableTimeframes.map { it.value }
        assertEquals(values.size, values.toSet().size)
    }
}
