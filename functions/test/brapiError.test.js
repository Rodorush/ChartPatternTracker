const {test} = require("node:test");
const assert = require("node:assert/strict");
const {isIntervalNotInPlan} = require("../brapiError");

const PLAN_REFUSAL = {
  error: true,
  code: "INVALID_INTERVAL",
  message: "O intervalo \"1m\" não está disponível no seu plano.",
  details: {feature: "__historical_interval__", requiredPlan: "startup"},
};

test("reconhece a recusa do intervalo pelo plano", () => {
  assert.equal(isIntervalNotInPlan(400, PLAN_REFUSAL), true);
});

test("400 com outro código não é recusa por plano", () => {
  assert.equal(isIntervalNotInPlan(400, {error: true, code: "OTHER"}), false);
  assert.equal(isIntervalNotInPlan(400, {error: true}), false);
});

test("INVALID_INTERVAL sem o recurso do plano não é recusa por plano", () => {
  const invalid = [
    {code: "INVALID_INTERVAL"},
    {code: "INVALID_INTERVAL", details: null},
    {code: "INVALID_INTERVAL", details: "__historical_interval__"},
    {code: "INVALID_INTERVAL", details: {feature: "__historical_range__"}},
  ];
  for (const body of invalid) {
    assert.equal(isIntervalNotInPlan(400, body), false, JSON.stringify(body));
  }
});

test("outros status não são recusa por plano", () => {
  for (const status of [401, 402, 403, 404, 429, 500]) {
    assert.equal(isIntervalNotInPlan(status, PLAN_REFUSAL), false, status);
  }
});

test("erro sem resposta não é recusa por plano", () => {
  assert.equal(isIntervalNotInPlan(undefined, undefined), false);
  assert.equal(isIntervalNotInPlan(undefined, PLAN_REFUSAL), false);
});

test("corpo fora do formato não é recusa por plano", () => {
  for (const body of [null, undefined, "INVALID_INTERVAL", 400, []]) {
    assert.equal(isIntervalNotInPlan(400, body), false, String(body));
  }
});
