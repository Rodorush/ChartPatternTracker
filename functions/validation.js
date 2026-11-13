/**
 * Validação da entrada da `updateCandlesticks`. Fica separada da função
 * para ser testada sem o SDK do Firebase.
 */

/**
 * Formato dos tickers da B3 que a Brapi lista: letras maiúsculas e dígitos.
 * O valor entra no caminho da URL da Brapi e no caminho do documento no
 * Firestore, então nada fora desse alfabeto pode passar.
 */
const TICKER_PATTERN = /^[A-Z0-9]{4,10}$/;

/**
 * Intervalos que a Brapi conhece. É a mesma lista de `TimeframeItem` no app;
 * o valor também nomeia a subcoleção de velas no Firestore.
 */
const TIMEFRAMES = Object.freeze([
  "1m", "2m", "5m", "15m", "30m", "1h", "1d", "1wk", "1mo",
]);

/**
 * @param {*} ticker valor recebido do cliente.
 * @return {boolean} se é um ticker aceito.
 */
function isValidTicker(ticker) {
  return typeof ticker === "string" && TICKER_PATTERN.test(ticker);
}

/**
 * @param {*} timeframe valor recebido do cliente.
 * @return {boolean} se é um intervalo aceito.
 */
function isValidTimeframe(timeframe) {
  return typeof timeframe === "string" && TIMEFRAMES.includes(timeframe);
}

/**
 * Confere o payload da chamada.
 *
 * @param {*} params payload enviado pelo cliente.
 * @return {?string} nome do primeiro campo inválido, ou `null` se o payload
 * é aceito. `brapiToken` é obrigatório: a função não tem token próprio, para
 * que ninguém gaste a cota de outra pessoa.
 */
function findInvalidField(params) {
  if (params === null || typeof params !== "object") return "payload";
  if (!isValidTicker(params.ticker)) return "ticker";
  if (!isValidTimeframe(params.timeframe)) return "timeframe";
  const token = params.brapiToken;
  if (typeof token !== "string" || token.length === 0) return "brapiToken";
  return null;
}

module.exports = {
  TIMEFRAMES,
  isValidTicker,
  isValidTimeframe,
  findInvalidField,
};
