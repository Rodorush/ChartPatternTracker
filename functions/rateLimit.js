/**
 * Cota diária de chamadas por usuário da `updateCandlesticks`. Fica separada
 * da função para ser testada sem o SDK do Firebase.
 */

/**
 * Chamadas por usuário por dia (UTC). O app só chama a função para o par de
 * ativo e tempo sem velas do dia no Room nem no Firestore, um par por vez, e
 * o Firestore é compartilhado: o limite cobre várias varreduras de dezenas
 * de ativos. Quem passa dele continua com dados, porque o app busca direto
 * na Brapi quando a função recusa.
 */
const DAILY_CALL_LIMIT = 300;

const ONE_DAY_MS = 24 * 60 * 60 * 1000;

/**
 * @param {Date} now instante da chamada.
 * @return {string} dia em UTC, no formato `AAAA-MM-DD`.
 */
function utcDay(now) {
  return now.toISOString().slice(0, 10);
}

/**
 * Decide se mais uma chamada cabe na cota do dia e calcula o documento a
 * gravar em `rate_limits/{uid}`.
 *
 * @param {*} state documento atual, ou `undefined` se ainda não existe. Dia
 * diferente do de hoje ou contagem malformada contam como zero.
 * @param {Date} now instante da chamada.
 * @param {number} limit chamadas permitidas por dia.
 * @return {{allowed: boolean, next: ?{day: string, count: number,
 * expireAt: Date}}} `next` é o documento a gravar, ou `null` quando a
 * chamada é recusada e nada muda. `expireAt` é a meia-noite UTC seguinte,
 * campo da política de TTL que apaga o documento depois que o dia acaba.
 */
function consumeQuota(state, now, limit = DAILY_CALL_LIMIT) {
  const day = utcDay(now);
  const sameDay = state !== null && typeof state === "object" &&
    state.day === day && Number.isInteger(state.count) && state.count >= 0;
  const count = sameDay ? state.count : 0;
  if (count >= limit) return {allowed: false, next: null};
  const expireAt = new Date(Date.parse(`${day}T00:00:00Z`) + ONE_DAY_MS);
  return {allowed: true, next: {day, count: count + 1, expireAt}};
}

module.exports = {
  DAILY_CALL_LIMIT,
  utcDay,
  consumeQuota,
};
