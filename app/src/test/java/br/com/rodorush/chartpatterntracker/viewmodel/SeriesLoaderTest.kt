package br.com.rodorush.chartpatterntracker.viewmodel

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.IntervalNotInPlanException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SeriesLoaderTest {

    private val fresh = listOf(candle(2))
    private val direct = listOf(candle(3))
    private val saved = listOf(candle(1))

    private var brapiCalls = 0
    private var cacheCalls = 0

    private fun candle(time: Long) = Candlestick(time, 10f, 11f, 9f, 10.5f, 100)

    private suspend fun load(
        fromRepository: suspend () -> List<Candlestick>,
        fromBrapi: Result<List<Candlestick>>,
        fromStaleCache: List<Candlestick>
    ) = loadSeries(
        fromRepository = fromRepository,
        fromBrapi = { brapiCalls++; fromBrapi },
        fromStaleCache = { cacheCalls++; fromStaleCache }
    )

    @Test
    fun `serie do repositorio dispensa as outras fontes`() = runTest {
        val result = load({ fresh }, Result.success(direct), saved)

        assertEquals(fresh, result.candles)
        assertFalse(result.stale)
        assertEquals(0, brapiCalls)
        assertEquals(0, cacheCalls)
    }

    @Test
    fun `falha do repositorio passa para a Brapi direta`() = runTest {
        val result = load({ throw IOException("sem rede") }, Result.success(direct), saved)

        assertEquals(direct, result.candles)
        assertFalse(result.stale)
        assertEquals(1, brapiCalls)
        assertEquals(0, cacheCalls)
    }

    @Test
    fun `sem nenhuma fonte serve o Room vencido marcado como desatualizado`() = runTest {
        val result = load(
            { throw IOException("sem rede") },
            Result.failure(IOException("sem rede")),
            saved
        )

        assertEquals(saved, result.candles)
        assertTrue(result.stale)
        assertEquals(1, cacheCalls)
    }

    @Test
    fun `Brapi direta vazia tambem cai no Room vencido`() = runTest {
        val result = load({ emptyList() }, Result.success(emptyList()), saved)

        assertEquals(saved, result.candles)
        assertTrue(result.stale)
    }

    @Test
    fun `sem nenhuma fonte e sem Room relanca a falha da Brapi direta`() = runTest {
        val brapiFailure = IOException("sem rede")

        val e = runCatching {
            load({ throw IOException("função") }, Result.failure(brapiFailure), emptyList())
        }.exceptionOrNull()

        assertSame(brapiFailure, e)
        assertEquals(1, cacheCalls)
    }

    @Test
    fun `fontes vazias sem falha devolvem serie vazia e atual`() = runTest {
        val result = load({ emptyList() }, Result.success(emptyList()), emptyList())

        assertTrue(result.candles.isEmpty())
        assertFalse(result.stale)
    }

    @Test
    fun `recusa do plano encerra a busca sem Brapi direta nem Room`() = runTest {
        val refusal = IntervalNotInPlanException("WEGE3", "1wk", RuntimeException("failed-precondition"))

        val e = runCatching { load({ throw refusal }, Result.success(direct), saved) }
            .exceptionOrNull()

        assertSame(refusal, e)
        assertEquals(0, brapiCalls)
        assertEquals(0, cacheCalls)
    }

    @Test
    fun `cancelamento dentro do Result da Brapi direta propaga sem ler o Room`() = runTest {
        val cancellation = CancellationException("varredura cancelada")

        val e = runCatching {
            load({ throw IOException("sem rede") }, Result.failure(cancellation), saved)
        }.exceptionOrNull()

        assertSame(cancellation, e)
        assertEquals(0, cacheCalls)
    }
}
