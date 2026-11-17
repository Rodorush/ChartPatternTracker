package br.com.rodorush.chartpatterntracker.util.provider

import br.com.rodorush.chartpatterntracker.api.BrapiApiService.QuoteListResponse
import br.com.rodorush.chartpatterntracker.api.BrapiApiService.StockQuote
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class AssetListTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File
    private lateinit var cache: AssetListCache

    private val petr = quote("PETR4")
    private val vale = quote("VALE3")

    private fun quote(ticker: String) =
        StockQuote(ticker, "Nome $ticker", 10.0, 1.5, "https://icons.brapi.dev/icons/$ticker.svg")

    @Before
    fun setUp() {
        file = File(folder.root, "brapi_quote_list.json")
        cache = AssetListCache(file)
    }

    @Test
    fun `lista gravada volta igual`() = runTest {
        cache.save(listOf(petr, vale))

        assertEquals(listOf(petr, vale), cache.load())
    }

    @Test
    fun `sem arquivo a lista salva vem vazia`() = runTest {
        assertEquals(emptyList<StockQuote>(), cache.load())
    }

    @Test
    fun `arquivo corrompido vira lista vazia`() = runTest {
        file.writeText("lixo{")
        assertEquals(emptyList<StockQuote>(), cache.load())

        file.writeText("{}")
        assertEquals(emptyList<StockQuote>(), cache.load())
    }

    @Test
    fun `gravar de novo substitui a lista sem deixar o temporario`() = runTest {
        cache.save(listOf(petr, vale))
        cache.save(listOf(vale))

        assertEquals(listOf(vale), cache.load())
        assertFalse(File(folder.root, "brapi_quote_list.json.tmp").exists())
    }

    @Test
    fun `lista da Brapi e devolvida e gravada`() = runTest {
        val result = loadQuoteList({ listOf(petr) }, cache, useLastLoad = true)

        assertEquals(listOf(petr), result)
        assertEquals(listOf(petr), cache.load())
    }

    @Test
    fun `falha da Brapi devolve a ultima carga`() = runTest {
        cache.save(listOf(petr, vale))

        val result = loadQuoteList({ throw IOException("sem rede") }, cache, useLastLoad = true)

        assertEquals(listOf(petr, vale), result)
    }

    @Test
    fun `falha da Brapi sem lista salva devolve vazia`() = runTest {
        val result = loadQuoteList({ throw IOException("sem rede") }, cache, useLastLoad = true)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `lista vazia da Brapi devolve a ultima carga sem apaga-la`() = runTest {
        cache.save(listOf(petr))

        val result = loadQuoteList({ emptyList() }, cache, useLastLoad = true)

        assertEquals(listOf(petr), result)
        assertEquals(listOf(petr), cache.load())
    }

    @Test
    fun `sem useLastLoad a falha da Brapi devolve vazia mesmo com lista salva`() = runTest {
        cache.save(listOf(petr))

        val result = loadQuoteList({ throw IOException("sem rede") }, cache, useLastLoad = false)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `sem useLastLoad a lista da Brapi tambem e gravada`() = runTest {
        loadQuoteList({ listOf(vale) }, cache, useLastLoad = false)

        assertEquals(listOf(vale), cache.load())
    }

    @Test
    fun `entrada nula ou sem ticker sai nas duas fontes`() = runTest {
        // O Gson ignora a nulidade do Kotlin, como na resposta real da Brapi.
        val json = """{"stocks":[{"name":"Sem ticker"},null,{"stock":"PETR4","name":"Petrobras"}]}"""
        val fromBrapi = Gson().fromJson(json, QuoteListResponse::class.java).stocks

        val direct = loadQuoteList({ fromBrapi }, cache, useLastLoad = true)
        assertEquals(listOf("PETR4"), direct.map { it.stock })

        file.writeText(json)
        val saved = loadQuoteList({ throw IOException("sem rede") }, cache, useLastLoad = true)
        assertEquals(listOf("PETR4"), saved.map { it.stock })
    }

    @Test
    fun `cancelamento e relancado em vez de virar lista salva`() = runTest {
        cache.save(listOf(petr))
        val cancellation = CancellationException("tela fechada")

        val e = runCatching {
            loadQuoteList({ throw cancellation }, cache, useLastLoad = true)
        }.exceptionOrNull()

        assertSame(cancellation, e)
    }
}
