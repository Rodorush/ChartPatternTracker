/**
 * Conversão do histórico da Brapi nas velas que a `updateCandlesticks` grava
 * e devolve. Fica separada da função para ser testada sem o SDK do Firebase.
 */

/**
 * @param {Array<Object>} history `historicalDataPrice` da resposta da Brapi,
 * com `date` em segundos.
 * @param {number} fetchedAt instante da consulta à Brapi, em ms epoch. Vai em
 * toda vela: a última da série pode ter sido lida com o período ainda aberto,
 * e é por esse carimbo que o app sabe se precisa buscá-la de novo depois do
 * fechamento.
 * @return {Array<Object>} velas `{time, open, high, low, close, volume,
 * fetchedAt}`, com `time` em ms epoch, que é a unidade do app.
 */
function toCandlesticks(history, fetchedAt) {
  return history.map((candle) => ({
    time: candle.date * 1000,
    open: candle.open,
    high: candle.high,
    low: candle.low,
    close: candle.close,
    volume: candle.volume,
    fetchedAt,
  }));
}

module.exports = {toCandlesticks};
