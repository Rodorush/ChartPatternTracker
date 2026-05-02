package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Bullish Hammer — reversão de alta (Nison; Bulkowski, *Encyclopedia of Candlestick Charts*).
 *   1. Vela única com corpo pequeno (≤ 40% do range total da vela).
 *   2. Sombra inferior longa (≥ 2× o corpo).
 *   3. Sombra superior pequena (≤ tamanho do corpo).
 * O contexto de tendência de baixa anterior é responsabilidade do consumidor (não verificado aqui).
 */
class BullishHammerDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        for (candle in candlesticks) {
            val length = candle.high - candle.low
            if (length <= 0f) continue
            val body = abs(candle.close - candle.open)
            val lowerShadow = min(candle.open, candle.close) - candle.low
            val upperShadow = candle.high - max(candle.open, candle.close)
            val smallBody = body / length < MAX_BODY_RATIO
            val longLowerShadow = lowerShadow >= body * MIN_LOWER_SHADOW_TO_BODY
            val shortUpperShadow = upperShadow <= body
            if (smallBody && longLowerShadow && shortUpperShadow) {
                occurrences.add(PatternOccurrence(listOf(candle)))
            }
        }
        return occurrences
    }

    private companion object {
        const val MAX_BODY_RATIO = 0.4f
        const val MIN_LOWER_SHADOW_TO_BODY = 2f
    }
}
