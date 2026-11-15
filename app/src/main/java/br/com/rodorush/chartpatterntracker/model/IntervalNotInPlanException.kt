package br.com.rodorush.chartpatterntracker.model

import com.google.firebase.functions.FirebaseFunctionsException

/**
 * O plano do token da Brapi não libera o tempo gráfico pedido, e a `updateCandlesticks`
 * devolveu a recusa. É uma resposta esperada, não um defeito: quem a recebe não registra
 * erro no Crashlytics nem tenta a Brapi direta, que recusaria o mesmo intervalo.
 *
 * @param cause a [FirebaseFunctionsException] original da chamada; `null` quando a recusa vem
 * de [RefusedPairs], que lembra o par já recusado e não chama a função de novo.
 */
class IntervalNotInPlanException(
    val ticker: String,
    val timeframe: String,
    cause: Throwable? = null
) : Exception("Intervalo $timeframe fora do plano do token da Brapi ($ticker)", cause)

/**
 * Valor de `details.reason` que a `updateCandlesticks` manda na recusa por plano. É o mesmo
 * `PLAN_INTERVAL_REASON` de `functions/brapiError.js`.
 */
internal const val PLAN_INTERVAL_REASON = "plan-interval"

/**
 * Diz se o erro da `updateCandlesticks` é a recusa do intervalo pelo plano do token:
 * código `FAILED_PRECONDITION` com `details.reason` igual a [PLAN_INTERVAL_REASON].
 *
 * @param codeName o `name` de [FirebaseFunctionsException.code]. Vai como texto porque o
 * enum `Code` usa `android.util.SparseArray` na inicialização e não carrega no teste
 * unitário da JVM.
 * @param details o `details` da [FirebaseFunctionsException], que o SDK entrega como `Map`
 * quando a função manda um objeto.
 */
internal fun isIntervalNotInPlan(codeName: String, details: Any?): Boolean =
    codeName == "FAILED_PRECONDITION" &&
        (details as? Map<*, *>)?.get("reason") == PLAN_INTERVAL_REASON
