package br.com.rodorush.chartpatterntracker.model

import br.com.rodorush.customlogs.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Pares de ativo e tempo gráfico que o plano do token da Brapi já recusou enquanto o processo
 * do app vive.
 *
 * A `updateCandlesticks` desconta a chamada da cota diária do usuário antes de consultar a
 * Brapi, então cada recusa por plano gasta uma chamada, e a mesma recusa voltaria em toda
 * varredura. A chave é o par, não só o tempo gráfico: a recusa depende do ativo, e há ativos
 * que o plano libera em todos os tempos enquanto recusa outros no mesmo tempo.
 */
internal class RefusedPairs {
    private val pairs: MutableSet<Pair<String, String>> = ConcurrentHashMap.newKeySet()

    /**
     * Executa [call], a chamada à `updateCandlesticks` para o par, a menos que o par já tenha
     * sido recusado: nesse caso lança [IntervalNotInPlanException] sem executá-la. Quando
     * [call] lança [IntervalNotInPlanException], o par fica lembrado e a exceção segue para o
     * chamador. As demais exceções passam sem deixar registro.
     */
    suspend fun <T> guard(ticker: String, timeframe: String, call: suspend () -> T): T {
        val pair = ticker to timeframe
        if (pair in pairs) {
            Log.d(TAG, "updateCandlesticks($ticker,$timeframe) não chamada: par já recusado pelo plano")
            throw IntervalNotInPlanException(ticker, timeframe)
        }
        try {
            return call()
        } catch (e: IntervalNotInPlanException) {
            pairs += pair
            throw e
        }
    }

    private companion object {
        const val TAG = "RefusedPairs"
    }
}
