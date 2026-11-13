const {test} = require("node:test");
const assert = require("node:assert/strict");
const {
  TIMEFRAMES,
  isValidTicker,
  isValidTimeframe,
  findInvalidField,
} = require("../validation");

test("aceita tickers da B3", () => {
  for (const ticker of ["WEGE3", "PETR4", "BOVA11", "PETR4F", "A1MD34"]) {
    assert.equal(isValidTicker(ticker), true, ticker);
  }
});

test("recusa ticker fora do formato", () => {
  const invalid = [
    "", "abc", "wege3", "WEG", "WEGE3WEGE3W", "WEGE3/x", "../WEGE3",
    "WEGE3?token=1", "WEGE 3", "^BVSP", 12345, null, undefined, {}, ["WEGE3"],
  ];
  for (const ticker of invalid) {
    assert.equal(isValidTicker(ticker), false, String(ticker));
  }
});

test("aceita só os intervalos da lista", () => {
  for (const timeframe of TIMEFRAMES) {
    assert.equal(isValidTimeframe(timeframe), true, timeframe);
  }
  const invalid = ["", "1D", "60m", "3mo", "1d/x", 1, null, undefined];
  for (const timeframe of invalid) {
    assert.equal(isValidTimeframe(timeframe), false, String(timeframe));
  }
});

test("aponta o primeiro campo inválido do payload", () => {
  assert.equal(findInvalidField(null), "payload");
  assert.equal(findInvalidField("WEGE3"), "payload");
  assert.equal(findInvalidField({timeframe: "1d"}), "ticker");
  assert.equal(findInvalidField({ticker: "WEGE3"}), "timeframe");
  assert.equal(
      findInvalidField({ticker: "WEGE3", timeframe: "1d", brapiToken: 1}),
      "brapiToken",
  );
});

test("aceita payload válido, com ou sem token", () => {
  assert.equal(findInvalidField({ticker: "WEGE3", timeframe: "1d"}), null);
  assert.equal(
      findInvalidField({ticker: "WEGE3", timeframe: "1d", brapiToken: "x"}),
      null,
  );
  assert.equal(
      findInvalidField({ticker: "WEGE3", timeframe: "1d", brapiToken: null}),
      null,
  );
});
