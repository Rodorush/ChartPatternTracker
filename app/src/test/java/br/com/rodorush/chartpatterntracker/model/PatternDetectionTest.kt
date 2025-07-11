package br.com.rodorush.chartpatterntracker.model

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PatternDetectionTest {
    @Test
    fun detectHaramiAlta_countsOccurrencesCorrectly() = runBlocking {
        val candles = listOf(
            // Bearish candle
            Candlestick(time = 1L, open = 10f, high = 11f, low = 5f, close = 6f),
            // Bullish contained in previous
            Candlestick(time = 2L, open = 7f, high = 9f, low = 5.5f, close = 9f),
            // Another bearish candle
            Candlestick(time = 3L, open = 9f, high = 10f, low = 4f, close = 5f),
            // Another bullish contained
            Candlestick(time = 4L, open = 5.5f, high = 7f, low = 4.5f, close = 6.5f)
        )

        val occurrences = detectHaramiAltaStandalone(candles)

        assertEquals(2, occurrences.size)
        occurrences.forEach { assertEquals(2, it.candles.size) }
    }

    @Test
    fun detectBearishEngulfing_countsOccurrencesCorrectly() = runBlocking {
        val candles = listOf(
            Candlestick(time = 1L, open = 9f, high = 10.5f, low = 8.8f, close = 10f),
            Candlestick(time = 2L, open = 10.2f, high = 10.4f, low = 8.5f, close = 8.8f)
        )

        val detector = br.com.rodorush.chartpatterntracker.model.pattern.BearishEngulfingDetector()
        val occurrences = detector.detect(candles)

        assertEquals(1, occurrences.size)
        occurrences.forEach { assertEquals(2, it.candles.size) }
    }

    @Test
    fun detectMarteloDeAlta_countsOccurrencesCorrectly() = runBlocking {
        val candles = listOf(
            Candlestick(time = 1L, open = 10f, high = 10.3f, low = 8f, close = 10.2f)
        )

        val detector = br.com.rodorush.chartpatterntracker.model.pattern.MarteloDeAltaDetector()
        val occurrences = detector.detect(candles)

        assertEquals(1, occurrences.size)
        occurrences.forEach { assertEquals(1, it.candles.size) }
    }

    @Test
    fun detectTresForaDeAlta_countsOccurrencesCorrectly() = runBlocking {
        val candles = listOf(
            Candlestick(time = 1L, open = 10f, high = 10.5f, low = 9.5f, close = 9.7f),
            Candlestick(time = 2L, open = 9.6f, high = 10.6f, low = 9.4f, close = 10.8f),
            Candlestick(time = 3L, open = 10.7f, high = 11.2f, low = 10.6f, close = 11.1f)
        )

        val detector = br.com.rodorush.chartpatterntracker.model.pattern.TresForaDeAltaDetector()
        val occurrences = detector.detect(candles)

        assertEquals(1, occurrences.size)
        occurrences.forEach { assertEquals(3, it.candles.size) }
    }

fun detectHaramiAltaStandalone(candlesticks: List<Candlestick>): List<PatternOccurrence> {
    val occurrences = mutableListOf<PatternOccurrence>()
    if (candlesticks.size < 2) return occurrences
    for (i in 1 until candlesticks.size) {
        val current = candlesticks[i]
        val previous = candlesticks[i - 1]
        val isBullish = current.close > current.open
        val isBearish = previous.open > previous.close
        val isContained = current.open > previous.close && current.close < previous.open
        if (isBullish && isBearish && isContained) {
            occurrences.add(PatternOccurrence(listOf(previous, current)))
        }
    }
    return occurrences
}
}
