'use strict';

const { test, before, after } = require('node:test');
const assert = require('node:assert');
const app = require('../src/app');

let server;
let base;

before(async () => {
  server = app.listen(0);
  await new Promise((resolve) => server.once('listening', resolve));
  base = `http://127.0.0.1:${server.address().port}/api/billing`;
});

after(() => server.close());

const json = (method, body) => ({ method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });

test('lists the tariffs', async () => {
  const res = await fetch(`${base}/tariffs`);
  assert.strictEqual(res.status, 200);
  assert.strictEqual((await res.json()).length, 4);
});

test('quotes a stay with free minutes', async () => {
  const res = await fetch(`${base}/quote`, json('POST', { spotType: 'handicapped', minutes: 90 }));
  assert.deepStrictEqual(await res.json(), {
    spotType: 'HANDICAPPED', minutes: 90, freeMinutes: 30, billedMinutes: 60,
    pricePerHour: 0.5, amount: 0.5, currency: 'TND',
  });
});

test('validates, updates, deletes and recreates a tariff', async () => {
  assert.strictEqual((await fetch(`${base}/tariffs`, json('POST', { spotType: 'EV', pricePerHour: 3 }))).status, 409);
  assert.strictEqual((await fetch(`${base}/tariffs/EV`, json('PUT', { pricePerHour: -1 }))).status, 400);
  const updated = await (await fetch(`${base}/tariffs/EV`, json('PUT', { pricePerHour: 2.5 }))).json();
  assert.strictEqual(updated.pricePerHour, 2.5);
  assert.strictEqual((await fetch(`${base}/tariffs/EV`, { method: 'DELETE' })).status, 204);
  assert.strictEqual((await fetch(`${base}/tariffs/EV`)).status, 404);
  const created = await fetch(`${base}/tariffs`, json('POST', { spotType: 'EV', pricePerHour: 2 }));
  assert.strictEqual(created.status, 201);
  assert.strictEqual(created.headers.get('location'), '/api/billing/tariffs/EV');
});

test('rejects malformed JSON', async () => {
  const res = await fetch(`${base}/quote`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{' });
  assert.strictEqual(res.status, 400);
});
