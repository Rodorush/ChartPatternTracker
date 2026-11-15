package br.com.rodorush.chartpatterntracker.model

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RefusedPairsTest {

    private val refused = RefusedPairs()
    private var calls = 0

    /** Simula a `updateCandlesticks` recusando o par pelo plano. */
    private fun refuse(ticker: String, timeframe: String): Nothing {
        calls++
        throw IntervalNotInPlanException(ticker, timeframe, RuntimeException("failed-precondition"))
    }

    private fun answer(): List<Candlestick> {
        calls++
        return emptyList()
    }

    @Test
    fun `primeira recusa chama a funcao e repassa a excecao`() = runTest {
        val e = runCatching { refused.guard("WEGE3", "1wk") { refuse("WEGE3", "1wk") } }
            .exceptionOrNull()

        assertEquals(1, calls)
        assertEquals(IntervalNotInPlanException::class.java, e?.javaClass)
    }

    @Test
    fun `par recusado nao chama a funcao de novo`() = runTest {
        runCatching { refused.guard("WEGE3", "1wk") { refuse("WEGE3", "1wk") } }

        val e = runCatching { refused.guard("WEGE3", "1wk") { answer() } }.exceptionOrNull()

        assertEquals(1, calls)
        val notInPlan = e as? IntervalNotInPlanException
        assertEquals("WEGE3", notInPlan?.ticker)
        assertEquals("1wk", notInPlan?.timeframe)
        assertNull(notInPlan?.cause)
    }

    @Test
    fun `mesmo tempo grafico em outro ativo segue chamando a funcao`() = runTest {
        runCatching { refused.guard("WEGE3", "1wk") { refuse("WEGE3", "1wk") } }

        val result = refused.guard("PETR4", "1wk") { answer() }

        assertEquals(2, calls)
        assertEquals(emptyList<Candlestick>(), result)
    }

    @Test
    fun `outro tempo grafico do mesmo ativo segue chamando a funcao`() = runTest {
        runCatching { refused.guard("WEGE3", "1wk") { refuse("WEGE3", "1wk") } }

        refused.guard("WEGE3", "1d") { answer() }

        assertEquals(2, calls)
    }

    @Test
    fun `outra falha nao fica lembrada`() = runTest {
        val failure = IllegalStateException("internal")
        val e = runCatching {
            refused.guard<List<Candlestick>>("WEGE3", "1wk") { calls++; throw failure }
        }.exceptionOrNull()
        assertSame(failure, e)

        refused.guard("WEGE3", "1wk") { answer() }

        assertEquals(2, calls)
    }
}
