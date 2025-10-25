package br.com.rodorush.chartpatterntracker.model.pattern

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Detecta o padrão Harami Cross (variação do Harami altista em que a segunda vela é um doji).
 *
 * Regras implementadas:
 * 1. Primeira vela deve ser de baixa e com corpo relativamente grande em relação ao range total.
 * 2. Segunda vela precisa ser um doji (corpo <= 10% do range) totalmente contido no corpo da vela anterior.
 * 3. Quando as condições são satisfeitas, registra a dupla de velas como ocorrência.
 */
class HaramiCrossDetector : PatternDetector {
    override fun detect(candlesticks: List<Candlestick>): List<PatternOccurrence> {
        val occurrences = mutableListOf<PatternOccurrence>()
        if (candlesticks.size < 2) return occurrences

        for (i in 1 until candlesticks.size) {
            val previous = candlesticks[i - 1]
            val current = candlesticks[i]

            val previousRange = previous.high - previous.low
            val previousBody = abs(previous.open - previous.close)
            if (previousRange <= 0f) continue
            val previousBodyRatio = previousBody / previousRange
            val previousIsBearish = previous.close < previous.open
            val hasStrongPreviousBody = previousIsBearish && previousBodyRatio >= MIN_PREVIOUS_BODY_RATIO

            val currentRange = current.high - current.low
            val currentBody = abs(current.open - current.close)
            val isDoji = currentRange > 0f && currentBody <= currentRange * MAX_DOJI_BODY_RATIO

            val upperBody = max(previous.open, previous.close)
            val lowerBody = min(previous.open, previous.close)
            val dojiInsidePreviousBody = current.high <= upperBody && current.low >= lowerBody

            if (hasStrongPreviousBody && isDoji && dojiInsidePreviousBody) {
                occurrences.add(PatternOccurrence(listOf(previous, current)))
            }
        }

        return occurrences
    }

    companion object {
        private const val MIN_PREVIOUS_BODY_RATIO = 0.6f
        private const val MAX_DOJI_BODY_RATIO = 0.1f
    }
}

