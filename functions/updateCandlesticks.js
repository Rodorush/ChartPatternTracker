const {onCall, HttpsError} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const axios = require("axios");
const {findInvalidField} = require("./validation");

admin.initializeApp();

/**
 * Busca na Brapi as velas dos últimos 3 meses de um ativo, grava em
 * `candlesticks/{ticker}/{timeframe}` e devolve a lista ao app.
 *
 * Só atende usuário logado e só aceita `ticker` e `timeframe` válidos
 * (ver `validation.js`). O token da Brapi vem do payload (`brapiToken`) ou
 * da variável de ambiente `BRAPI_TOKEN`, e nunca vai para o log: nem o
 * payload, nem o objeto de erro do axios, que carrega o token nos parâmetros
 * da requisição.
 */
exports.updateCandlesticks = onCall(async (request) => {
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

  try {
    const token = brapiToken || process.env.BRAPI_TOKEN;
    if (!token) {
      throw new Error("Brapi token not configured");
    }

    const response = await axios.get(`https://brapi.dev/api/quote/${ticker}`, {
      params: {
        range: "3mo",
        interval: timeframe,
        token: token,
      },
    });

    const history = response.data.results[0].historicalDataPrice;
    const candlesticks = history.map((candle) => ({
      // A Brapi devolve a data em segundos; o app trabalha em milissegundos.
      time: new Date(candle.date * 1000).getTime(),
      open: candle.open,
      high: candle.high,
      low: candle.low,
      close: candle.close,
      volume: candle.volume,
    }));

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
    // `config.params`.
    logger.error("Erro ao atualizar candlesticks", {
      ticker,
      timeframe,
      mensagem: error.message,
      status: error.response ? error.response.status : undefined,
      resposta: error.response ? error.response.data : undefined,
    });
    throw new HttpsError(
        "internal",
        `Failed to update candlesticks: ${error.message}`,
    );
  }
});
