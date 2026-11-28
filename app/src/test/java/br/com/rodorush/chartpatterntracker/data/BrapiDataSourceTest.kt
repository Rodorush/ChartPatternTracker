package br.com.rodorush.chartpatterntracker.data

import br.com.rodorush.chartpatterntracker.api.BrapiResponse
import br.com.rodorush.chartpatterntracker.api.BrapiResult
import br.com.rodorush.chartpatterntracker.api.HistoricalData
import br.com.rodorush.chartpatterntracker.model.Candlestick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrapiDataSourceTest {

    @Test
    fun `historicalDataPrice vazio e serie sem velas`() {
        // É como a Brapi responde para o ativo sem negócios no período (SMAC em 1d).
        val result = BrapiResponse(listOf(BrapiResult(emptyList()))).toCandles()

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Candlestick>(), result.getOrNull())
    }

    @Test
    fun `resposta sem historicalDataPrice e falha`() {
        val result = BrapiResponse(listOf(BrapiResult(null))).toCandles()

        assertTrue(result.isFailure)
    }

    @Test
    fun `resposta sem results e falha`() {
        assertTrue(BrapiResponse(emptyList()).toCandles().isFailure)
        assertTrue(BrapiResponse(null).toCandles().isFailure)
    }

    @Test
    fun `velas saem com o horario em milissegundos`() {
        val response = BrapiResponse(
            listOf(BrapiResult(listOf(HistoricalData(1_700_000_000, 10.0, 11.0, 9.0, 10.5, 100))))
        )

        val candles = response.toCandles().getOrThrow()

        assertEquals(listOf(Candlestick(1_700_000_000_000, 10f, 11f, 9f, 10.5f, 100)), candles)
    }
}
