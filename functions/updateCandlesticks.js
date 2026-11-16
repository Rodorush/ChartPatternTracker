const {onCall, HttpsError} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const axios = require("axios");
const {findInvalidField} = require("./validation");
const {consumeQuota} = require("./rateLimit");
const {PLAN_INTERVAL_REASON, isIntervalNotInPlan} = require("./brapiError");
const {toCandlesticks} = require("./candles");

admin.initializeApp();

/**
 * Opções da função. `maxInstances` é o teto de instâncias simultâneas; o de
 * chamadas simultâneas é ele vezes a concorrência por instância. O app faz
 * uma chamada por vez por usuário.
 */
const OPTIONS = {maxInstances: 5};

/**
 * Conta mais uma chamada do usuário em `rate_limits/{uid}`, numa transação,
 * pela cota de `rateLimit.js`. O documento só existe para a função: o
 * cliente não o lê nem grava (`firestore.rules`).
 *
 * @param {string} uid usuário autenticado da chamada.
 * @return {Promise<boolean>} se a chamada cabe na cota do dia.
 */
async function reserveCall(uid) {
  const db = admin.firestore();
  const ref = db.collection("rate_limits").doc(uid);
  return db.runTransaction(async (tx) => {
    const snapshot = await tx.get(ref);
    const {allowed, next} = consumeQuota(snapshot.data(), new Date());
    if (allowed) tx.set(ref, next);
    return allowed;
  });
}

/**
 * Busca na Brapi as velas dos últimos 3 meses de um ativo, grava em
 * `candlesticks/{ticker}/{timeframe}` e devolve a lista ao app. Cada vela
 * leva `fetchedAt`, o instante da consulta (`candles.js`): a Brapi devolve a
 * vela do período em andamento, e a chamada regrava os 3 meses, então só a
 * última vela da série pode estar parcial.
 *
 * Só atende usuário logado e só aceita `ticker` e `timeframe` válidos
 * (ver `validation.js`). Cada usuário tem uma cota diária de chamadas
 * (`rateLimit.js`); acima dela, a função recusa com `resource-exhausted`
 * antes de chamar a Brapi. Quando a Brapi recusa o intervalo porque o plano
 * do token não o libera (`brapiError.js`), a função responde
 * `failed-precondition` com `details.reason` igual a `PLAN_INTERVAL_REASON`,
 * para o app tratar a recusa como esperada; os demais erros saem como
 * `internal`. O token da Brapi vem sempre do payload
 * (`brapiToken`): a função não tem token próprio, então quem chama gasta a
 * própria cota. Ele nunca vai para o log: nem o payload, nem o objeto de
 * erro do axios, que carrega o token nos parâmetros da requisição.
 */
exports.updateCandlesticks = onCall(OPTIONS, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required");
  }

  const params = request.data;
  const invalidField = findInvalidField(params);
  if (invalidField) {
    logger.warn("Entrada recusada", {campo: invalidField});
    throw new HttpsError("invalid-argument", `Invalid ${invalidField}`);
  }
  const {ticker, timeframe, brapiToken} = params;

  let allowed;
  try {
    allowed = await reserveCall(request.auth.uid);
  } catch (error) {
    logger.error("Erro ao reservar a cota", {
      ticker,
      timeframe,
      mensagem: error.message,
    });
    throw new HttpsError("internal", "Failed to reserve quota");
  }
  if (!allowed) {
    logger.warn("Cota diária esgotada", {ticker, timeframe});
    throw new HttpsError("resource-exhausted", "Daily call limit reached");
  }

  try {
    // Antes da consulta: o carimbo nunca fica à frente do dado que acompanha.
    const fetchedAt = Date.now();
    const response = await axios.get(`https://brapi.dev/api/quote/${ticker}`, {
      params: {
        range: "3mo",
        interval: timeframe,
        token: brapiToken,
      },
    });

    const history = response.data.results[0].historicalDataPrice;
    const candlesticks = toCandlesticks(history, fetchedAt);

    const batch = admin.firestore().batch();
    candlesticks.forEach((candle) => {
      const docRef = admin.firestore()
          .collection("candlesticks")
          .doc(ticker)
          .collection(timeframe)
          .doc(candle.time.toString());
      batch.set(docRef, candle);
    });
    await batch.commit();

    logger.info("Velas gravadas", {
      ticker,
      timeframe,
      total: candlesticks.length,
    });
    return candlesticks;
  } catch (error) {
    // Só campos escolhidos: o objeto de erro do axios leva o token em
    // `config.params`, e o corpo da resposta não é nosso para garantir
    // que não o repita. Da Brapi entra só o texto de `message`.
    const status = error.response ? error.response.status : undefined;
    const body = error.response ? error.response.data : undefined;
    if (isIntervalNotInPlan(status, body)) {
      logger.warn("Intervalo fora do plano do token", {ticker, timeframe});
      throw new HttpsError(
          "failed-precondition",
          "Interval not available in the Brapi plan",
          {reason: PLAN_INTERVAL_REASON},
      );
    }
    const brapiMessage = body && typeof body.message === "string" ?
      body.message.slice(0, 200) : undefined;
    logger.error("Erro ao atualizar candlesticks", {
      ticker,
      timeframe,
      mensagem: error.message,
      status,
      brapi: brapiMessage,
    });
    throw new HttpsError(
        "internal",
        `Failed to update candlesticks: ${error.message}`,
    );
  }
});
