package br.com.rodorush.chartpatterntracker.model

import br.com.rodorush.chartpatterntracker.model.pattern.AbandonedBabyBearishDetector
import br.com.rodorush.chartpatterntracker.model.pattern.BearishEngulfingDetector
import br.com.rodorush.chartpatterntracker.model.pattern.BullishDojiStarDetector
import br.com.rodorush.chartpatterntracker.model.pattern.BullishHammerDetector
import br.com.rodorush.chartpatterntracker.model.pattern.EveningDojiStarDetector
import br.com.rodorush.chartpatterntracker.model.pattern.EveningStarDetector
import br.com.rodorush.chartpatterntracker.model.pattern.HaramiBullishDetector
import br.com.rodorush.chartpatterntracker.model.pattern.HaramiCrossDetector
import br.com.rodorush.chartpatterntracker.model.pattern.PatternDetector
import br.com.rodorush.chartpatterntracker.model.pattern.PatternDetectorRegistry
import br.com.rodorush.chartpatterntracker.model.pattern.ThreeOutsideDownDetector
import br.com.rodorush.chartpatterntracker.model.pattern.ThreeOutsideUpDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternDetectionTest {

    private fun candle(time: Long, open: Float, high: Float, low: Float, close: Float, volume: Long? = null) =
        Candlestick(time = time, open = open, high = high, low = low, close = close, volume = volume)

    // ----- Bearish Engulfing (id 9) -----

    @Test
    fun bearishEngulfing_detectsWhenBodyEngulfsPrevious() {
        val candles = listOf(
            candle(1, open = 9f, high = 10.5f, low = 8.8f, close = 10f),       // bullish
            candle(2, open = 10.2f, high = 10.4f, low = 8.5f, close = 8.8f)    // bearish que engulfa
        )
        val occurrences = BearishEngulfingDetector().detect(candles)
        assertEquals(1, occurrences.size)
        assertEquals(2, occurrences.first().candles.size)
    }

    @Test
    fun bearishEngulfing_rejectsWhenSecondBodyDoesNotEngulf() {
        val candles = listOf(
            candle(1, open = 9f, high = 10.5f, low = 8.8f, close = 10f),
            candle(2, open = 9.9f, high = 10.0f, low = 9.0f, close = 9.2f) // open <= prev close → não engulfa
        )
        assertTrue(BearishEngulfingDetector().detect(candles).isEmpty())
    }

    // ----- Harami Bullish (id 48) -----

    @Test
    fun haramiBullish_detectsBullishContainedInPriorBearishBody() {
        val candles = listOf(
            candle(1, open = 10f, high = 11f, low = 5f, close = 6f),  // bearish, body=4, range=6 → 0.66
            candle(2, open = 7f, high = 9f, low = 5.5f, close = 9f),  // bullish contido
            candle(3, open = 9f, high = 10f, low = 4f, close = 5f),   // bearish, body=4, range=6 → 0.66
            candle(4, open = 5.5f, high = 7f, low = 4.5f, close = 6.5f) // bullish contido
        )
        val occurrences = HaramiBullishDetector().detect(candles)
        assertEquals(2, occurrences.size)
        occurrences.forEach { assertEquals(2, it.candles.size) }
    }

    @Test
    fun haramiBullish_rejectsWhenPriorBodyIsShort() {
        // body/range = 1/6 → abaixo do limite de 60%
        val candles = listOf(
            candle(1, open = 10f, high = 11f, low = 5f, close = 9f),
            candle(2, open = 9.5f, high = 9.8f, low = 9.2f, close = 9.6f)
        )
        assertTrue(HaramiBullishDetector().detect(candles).isEmpty())
    }

    // ----- Bullish Hammer (id 46) -----

    @Test
    fun bullishHammer_detectsLongLowerShadowSmallBody() {
        val candles = listOf(
            candle(1, open = 10f, high = 10.3f, low = 8f, close = 10.2f)
        )
        val occurrences = BullishHammerDetector().detect(candles)
        assertEquals(1, occurrences.size)
        assertEquals(1, occurrences.first().candles.size)
    }

    @Test
    fun bullishHammer_rejectsLargeBody() {
        // body 5 / range 6 = 0.83 → corpo grande demais
        val candles = listOf(
            candle(1, open = 5f, high = 11f, low = 5f, close = 10f)
        )
        assertTrue(BullishHammerDetector().detect(candles).isEmpty())
    }

    // ----- Three Outside Up (id 58) -----

    @Test
    fun threeOutsideUp_detectsEngulfingFollowedByContinuation() {
        val candles = listOf(
            candle(1, open = 10f, high = 10.5f, low = 9.5f, close = 9.7f),    // bearish
            candle(2, open = 9.6f, high = 10.6f, low = 9.4f, close = 10.8f),  // bullish engulfa
            candle(3, open = 10.7f, high = 11.2f, low = 10.6f, close = 11.1f) // bullish continuação
        )
        val occurrences = ThreeOutsideUpDetector().detect(candles)
        assertEquals(1, occurrences.size)
        assertEquals(3, occurrences.first().candles.size)
    }

    @Test
    fun threeOutsideUp_rejectsWhenThirdNotHigherThanSecond() {
        val candles = listOf(
            candle(1, open = 10f, high = 10.5f, low = 9.5f, close = 9.7f),
            candle(2, open = 9.6f, high = 10.6f, low = 9.4f, close = 10.8f),
            candle(3, open = 10.7f, high = 11.0f, low = 10.5f, close = 10.6f) // close < second.close
        )
        assertTrue(ThreeOutsideUpDetector().detect(candles).isEmpty())
    }

    // ----- Three Outside Down (id 24) -----

    @Test
    fun threeOutsideDown_detectsBearishEngulfingFollowedByContinuation() {
        val candles = listOf(
            candle(1, open = 10f, high = 10.4f, low = 9.8f, close = 10.3f),   // bullish
            candle(2, open = 10.4f, high = 10.5f, low = 9.5f, close = 9.7f),  // bearish engulfa
            candle(3, open = 9.6f, high = 9.8f, low = 9.0f, close = 9.2f)     // bearish continua
        )
        val occurrences = ThreeOutsideDownDetector().detect(candles)
        assertEquals(1, occurrences.size)
        assertEquals(3, occurrences.first().candles.size)
    }

    @Test
    fun threeOutsideDown_rejectsWhenThirdClosesAboveSecondClose() {
        val candles = listOf(
            candle(1, open = 10f, high = 10.4f, low = 9.8f, close = 10.3f),
            candle(2, open = 10.4f, high = 10.5f, low = 9.5f, close = 9.7f),
            candle(3, open = 9.8f, high = 10.1f, low = 9.6f, close = 9.9f) // close > second.close
        )
        assertTrue(ThreeOutsideDownDetector().detect(candles).isEmpty())
    }

    // ----- Harami Cross (id 49) -----

    @Test
    fun haramiCross_detectsDojiInsideBearishBody() {
        val candles = listOf(
            candle(1, open = 10.5f, high = 10.8f, low = 8.4f, close = 8.6f), // bearish, body 1.9 / range 2.4 ≈ 0.79
            candle(2, open = 9.22f, high = 9.32f, low = 9.15f, close = 9.23f), // doji contido
            candle(3, open = 9.6f, high = 10.2f, low = 9.5f, close = 9.8f)
        )
        val occurrences = HaramiCrossDetector().detect(candles)
        assertEquals(1, occurrences.size)
        assertEquals(2, occurrences.first().candles.size)
    }

    @Test
    fun haramiCross_rejectsBullishSecondCandle() {
        val candles = listOf(
            candle(1, open = 10.5f, high = 10.8f, low = 8.4f, close = 8.6f),
            candle(2, open = 9.0f, high = 9.5f, low = 8.9f, close = 9.4f) // body grande, não é doji
        )
        assertTrue(HaramiCrossDetector().detect(candles).isEmpty())
    }

    // ----- Evening Star (id 11) -----

    @Test
    fun eveningStar_detectsSmallBodyStarReversal() {
        val candles = listOf(
            candle(1, open = 10f, high = 15f, low = 9.5f, close = 14f),       // bullish longa
            candle(2, open = 15.2f, high = 15.5f, low = 14.8f, close = 15f),  // estrela com gap-up
            candle(3, open = 14.8f, high = 15.0f, low = 10.5f, close = 11f)   // bearish abre dentro/abaixo, fecha < meio v1
        )
        val occurrences = EveningStarDetector().detect(candles)
        assertEquals(1, occurrences.size)
        assertEquals(3, occurrences.first().candles.size)
    }

    @Test
    fun eveningStar_rejectsWhenStarBodyIsNotSmall() {
        val candles = listOf(
            candle(1, open = 10f, high = 15f, low = 9.5f, close = 14f),
            candle(2, open = 14.5f, high = 18f, low = 14.4f, close = 17f), // body 2.5 > 50% do body 4 da v1
            candle(3, open = 17f, high = 17.2f, low = 11f, close = 11.5f)
        )
        assertTrue(EveningStarDetector().detect(candles).isEmpty())
    }

    // ----- Evening Doji Star (id 10) -----

    @Test
    fun eveningDojiStar_detectsDojiStarReversal() {
        val candles = listOf(
            candle(1, open = 10f, high = 15f, low = 9.5f, close = 14f),
            candle(2, open = 16f, high = 16.5f, low = 15.5f, close = 16.05f), // doji, gap-up completo
            candle(3, open = 15f, high = 15.4f, low = 11f, close = 11.5f)     // bearish abre abaixo do doji body
        )
        val occurrences = EveningDojiStarDetector().detect(candles)
        assertEquals(1, occurrences.size)
        assertEquals(3, occurrences.first().candles.size)
    }

    @Test
    fun eveningDojiStar_rejectsWhenSecondCandleIsNotDoji() {
        val candles = listOf(
            candle(1, open = 10f, high = 15f, low = 9.5f, close = 14f),
            candle(2, open = 15.2f, high = 17f, low = 15.1f, close = 16.8f), // body 1.6 / range 1.9 = 0.84 → não é doji
            candle(3, open = 15f, high = 15.4f, low = 11f, close = 11.5f)
        )
        assertTrue(EveningDojiStarDetector().detect(candles).isEmpty())
    }

    // ----- Bullish Doji Star (id 43) -----

    @Test
    fun bullishDojiStar_detectsDojiStarReversal() {
        val candles = listOf(
            candle(1, open = 15f, high = 15.5f, low = 10f, close = 10.5f),       // bearish longa
            candle(2, open = 9.5f, high = 9.6f, low = 9f, close = 9.55f),        // doji, gap-down completo (high < v1.low)
            candle(3, open = 10f, high = 14f, low = 9.8f, close = 13.5f)         // bullish abre acima do corpo do doji, fecha > meio v1
        )
        val occurrences = BullishDojiStarDetector().detect(candles)
        assertEquals(1, occurrences.size)
        assertEquals(3, occurrences.first().candles.size)
    }

    @Test
    fun bullishDojiStar_rejectsWhenGapDownIsIncomplete() {
        val candles = listOf(
            candle(1, open = 15f, high = 15.5f, low = 10f, close = 10.5f),
            candle(2, open = 10.4f, high = 10.5f, low = 10.1f, close = 10.45f), // high não fica abaixo de v1.low
            candle(3, open = 10.6f, high = 14f, low = 10.4f, close = 13.5f)
        )
        assertTrue(BullishDojiStarDetector().detect(candles).isEmpty())
    }

    // ----- Abandoned Baby Bearish (id 1) -----

    @Test
    fun abandonedBabyBearish_detectsIslandReversal() {
        val candles = listOf(
            candle(1, open = 10f, high = 15f, low = 9.5f, close = 14f),
            candle(2, open = 16f, high = 16.5f, low = 15.2f, close = 16.05f), // doji, gap-up isla
            candle(3, open = 15f, high = 15.1f, low = 10f, close = 10.5f)     // bearish, gap-down isla (high < v2.low)
        )
        val occurrences = AbandonedBabyBearishDetector().detect(candles)
        assertEquals(1, occurrences.size)
        assertEquals(3, occurrences.first().candles.size)
    }

    @Test
    fun abandonedBabyBearish_rejectsWithoutFullGapDown() {
        val candles = listOf(
            candle(1, open = 10f, high = 15f, low = 9.5f, close = 14f),
            candle(2, open = 16f, high = 16.5f, low = 15.2f, close = 16.05f),
            candle(3, open = 15f, high = 15.5f, low = 10f, close = 10.5f) // high 15.5 > v2.low 15.2 → sem gap
        )
        assertTrue(AbandonedBabyBearishDetector().detect(candles).isEmpty())
    }

    // ----- Boundary: todos os detectores devem retornar lista vazia para entrada vazia -----

    @Test
    fun allDetectors_returnEmptyForEmptyInput() {
        PatternDetectorRegistry.registeredIds().forEach { id ->
            val detector = PatternDetectorRegistry.get(id)
            assertTrue("Detector $id deveria existir", detector != null)
            assertTrue(
                "Detector $id deveria retornar vazio para lista vazia",
                detector!!.detect(emptyList()).isEmpty()
            )
        }
    }

    @Test
    fun threeCandleDetectors_returnEmptyForListSmallerThanThree() {
        val twoCandles = listOf(
            candle(1, 10f, 11f, 9f, 10.5f),
            candle(2, 10.5f, 11.5f, 10f, 11f)
        )
        val threeCandleDetectors: List<PatternDetector> = listOf(
            AbandonedBabyBearishDetector(),
            BullishDojiStarDetector(),
            EveningDojiStarDetector(),
            EveningStarDetector(),
            ThreeOutsideDownDetector(),
            ThreeOutsideUpDetector()
        )
        threeCandleDetectors.forEach { d ->
            assertTrue(
                "${d::class.simpleName} deveria retornar vazio com 2 velas",
                d.detect(twoCandles).isEmpty()
            )
        }
    }

    @Test
    fun registryHasAllTenDetectors() {
        assertEquals(10, PatternDetectorRegistry.registeredIds().size)
    }
}
