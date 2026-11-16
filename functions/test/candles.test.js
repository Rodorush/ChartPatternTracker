const {test} = require("node:test");
const assert = require("node:assert/strict");
const {toCandlesticks} = require("../candles");

const FETCHED_AT = Date.parse("2026-11-16T21:30:00Z");

// Últimas velas diárias de PETR4 na Brapi em 16/11/2026, 00:00 de Brasília.
const HISTORY = [
  {
    date: 1794538800, open: 44.89, high: 45.44, low: 44.62, close: 45.3,
    volume: 23431100, adjustedClose: 45.3,
  },
  {
    date: 1794798000, open: 45.1, high: 45.39, low: 43.65, close: 44.02,
    volume: 30506500, adjustedClose: 44.02,
  },
];

test("converte a data de segundos para milissegundos", () => {
  const candles = toCandlesticks(HISTORY, FETCHED_AT);
  assert.deepEqual(candles.map((c) => c.time), [1794538800000, 1794798000000]);
});

test("carimba toda vela com o instante da consulta", () => {
  const candles = toCandlesticks(HISTORY, FETCHED_AT);
  assert.deepEqual(candles.map((c) => c.fetchedAt), [FETCHED_AT, FETCHED_AT]);
});

test("leva só os campos que o app lê", () => {
  assert.deepEqual(toCandlesticks(HISTORY, FETCHED_AT)[1], {
    time: 1794798000000,
    open: 45.1,
    high: 45.39,
    low: 43.65,
    close: 44.02,
    volume: 30506500,
    fetchedAt: FETCHED_AT,
  });
});

test("histórico vazio devolve lista vazia", () => {
  assert.deepEqual(toCandlesticks([], FETCHED_AT), []);
});
