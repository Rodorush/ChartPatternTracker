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
 * o recurso (`feature: "__historical_interval__"`) e o plano exigido. Os dois
 * campos são exigidos: um `INVALID_INTERVAL` sem o recurso do plano é outra
 * falha e segue como erro.
 *
 * @param {number|undefined} status status HTTP da resposta, ou `undefined`
 * quando não houve resposta (erro de rede).
 * @param {*} body corpo da resposta de erro, como o axios entrega.
 * @return {boolean} se é a recusa por plano.
 */
function isIntervalNotInPlan(status, body) {
  if (status !== 400 || body === null || typeof body !== "object") {
    return false;
  }
  const details = body.details;
  return body.code === "INVALID_INTERVAL" &&
    details !== null &&
    typeof details === "object" &&
    details.feature === "__historical_interval__";
}

module.exports = {
  PLAN_INTERVAL_REASON,
  isIntervalNotInPlan,
};
