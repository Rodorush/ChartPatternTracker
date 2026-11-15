/**
 * Classificação dos erros da Brapi na `updateCandlesticks`. Fica separada da
 * função para ser testada sem o SDK do Firebase.
 */

/**
 * Valor de `details.reason` no `HttpsError` da recusa por plano. O app
 * compara com a mesma constante em `IntervalNotInPlanException.kt`.
 */
const PLAN_INTERVAL_REASON = "plan-interval";

/**
 * Diz se a Brapi recusou o intervalo porque o plano do token não o libera.
 * Nesse caso ela responde 400 com `code: "INVALID_INTERVAL"` e, em `details`,
 * o recurso (`__historical_interval__`) e o plano exigido. A função já valida
 * o intervalo contra a lista que a Brapi conhece (`validation.js`), então
 * esse 400 vem do plano, não de um valor inválido.
 *
 * @param {number|undefined} status status HTTP da resposta, ou `undefined`
 * quando não houve resposta (erro de rede).
 * @param {*} body corpo da resposta de erro, como o axios entrega.
 * @return {boolean} se é a recusa por plano.
 */
function isIntervalNotInPlan(status, body) {
  return status === 400 &&
    body !== null &&
    typeof body === "object" &&
    body.code === "INVALID_INTERVAL";
}

module.exports = {
  PLAN_INTERVAL_REASON,
  isIntervalNotInPlan,
};
