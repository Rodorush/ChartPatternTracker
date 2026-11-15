const {test} = require("node:test");
const assert = require("node:assert/strict");
const {DAILY_CALL_LIMIT, utcDay, consumeQuota} = require("../rateLimit");

const NOW = new Date("2026-11-15T13:45:00Z");
const NEXT_MIDNIGHT = new Date("2026-11-16T00:00:00Z");

test("o dia é contado em UTC", () => {
  assert.equal(utcDay(new Date("2026-11-15T23:59:59Z")), "2026-11-15");
  assert.equal(utcDay(new Date("2026-11-16T00:00:00Z")), "2026-11-16");
});

test("primeira chamada do dia começa a contagem", () => {
  assert.deepEqual(consumeQuota(undefined, NOW, 3), {
    allowed: true,
    next: {day: "2026-11-15", count: 1, expireAt: NEXT_MIDNIGHT},
  });
});

test("chamada no mesmo dia soma 1", () => {
  const result = consumeQuota({day: "2026-11-15", count: 1}, NOW, 3);
  assert.equal(result.allowed, true);
  assert.equal(result.next.count, 2);
});

test("a última chamada da cota passa e a seguinte é recusada", () => {
  const last = consumeQuota({day: "2026-11-15", count: 2}, NOW, 3);
  assert.equal(last.allowed, true);
  assert.equal(last.next.count, 3);
  assert.deepEqual(
      consumeQuota({day: "2026-11-15", count: 3}, NOW, 3),
      {allowed: false, next: null},
  );
});

test("o dia novo zera a contagem", () => {
  const result = consumeQuota({day: "2026-11-14", count: 3}, NOW, 3);
  assert.equal(result.allowed, true);
  assert.equal(result.next.day, "2026-11-15");
  assert.equal(result.next.count, 1);
});

test("documento malformado conta como zero", () => {
  const malformed = [
    null, "x", {}, {day: "2026-11-15"}, {day: "2026-11-15", count: "2"},
    {day: "2026-11-15", count: -1}, {day: "2026-11-15", count: 1.5},
  ];
  for (const state of malformed) {
    const result = consumeQuota(state, NOW, 3);
    assert.equal(result.allowed, true, JSON.stringify(state));
    assert.equal(result.next.count, 1, JSON.stringify(state));
  }
});

test("sem limite explícito, vale a cota padrão", () => {
  const atLimit = {day: "2026-11-15", count: DAILY_CALL_LIMIT};
  assert.equal(consumeQuota(atLimit, NOW).allowed, false);
  const belowLimit = {day: "2026-11-15", count: DAILY_CALL_LIMIT - 1};
  assert.equal(consumeQuota(belowLimit, NOW).allowed, true);
});
