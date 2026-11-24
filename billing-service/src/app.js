'use strict';

const express = require('express');
const client = require('prom-client');
const { SPOT_TYPES, tariffs, validate, quote } = require('./tariffs');

// Prometheus metrics (scraped on /metrics), like the Spring services' /actuator/prometheus.
const register = new client.Registry();
register.setDefaultLabels({ application: 'billing-service' });
client.collectDefaultMetrics({ register });
const httpRequests = new client.Histogram({
  name: 'http_server_requests_seconds',
  help: 'Duration of HTTP requests',
  labelNames: ['method', 'uri', 'status'],
  registers: [register],
});

const app = express();
app.use(express.json());

app.use((req, res, next) => {
  const end = httpRequests.startTimer();
  res.on('finish', () => {
    const uri = req.route ? req.baseUrl + req.route.path : 'UNKNOWN';
    end({ method: req.method, uri, status: res.statusCode });
  });
  next();
});

const router = express.Router();

function notFound(res, spotType) {
  return res.status(404).json({ status: 404, error: 'Not Found', message: `No tariff for ${spotType}` });
}

function spotTypeOf(value) {
  return typeof value === 'string' ? value.toUpperCase() : undefined;
}

// GET /api/billing/tariffs
router.get('/tariffs', (req, res) => {
  res.json([...tariffs.values()]);
});

// GET /api/billing/tariffs/EV
router.get('/tariffs/:spotType', (req, res) => {
  const tariff = tariffs.get(spotTypeOf(req.params.spotType));
  if (!tariff) return notFound(res, req.params.spotType);
  res.json(tariff);
});

// POST /api/billing/tariffs  {"spotType":"EV","pricePerHour":2.5}
router.post('/tariffs', (req, res) => {
  const spotType = spotTypeOf(req.body?.spotType);
  const errors = validate(req.body);
  if (!SPOT_TYPES.includes(spotType)) errors.push(`spotType must be one of ${SPOT_TYPES.join(', ')}`);
  if (errors.length) return res.status(400).json({ status: 400, error: 'Bad Request', messages: errors });
  if (tariffs.has(spotType)) {
    return res.status(409).json({ status: 409, error: 'Conflict', message: `A tariff for ${spotType} exists, use PUT` });
  }
  const tariff = {
    spotType,
    pricePerHour: req.body.pricePerHour,
    currency: req.body.currency ?? 'TND',
    freeMinutes: req.body.freeMinutes ?? 0,
  };
  tariffs.set(spotType, tariff);
  res.status(201).location(`${req.baseUrl}/tariffs/${spotType}`).json(tariff);
});

// PUT /api/billing/tariffs/EV  {"pricePerHour":2.5,"freeMinutes":10}
router.put('/tariffs/:spotType', (req, res) => {
  const tariff = tariffs.get(spotTypeOf(req.params.spotType));
  if (!tariff) return notFound(res, req.params.spotType);
  const errors = validate(req.body, { partial: true });
  if (errors.length) return res.status(400).json({ status: 400, error: 'Bad Request', messages: errors });
  const updated = { ...tariff };
  for (const field of ['pricePerHour', 'currency', 'freeMinutes']) {
    if (req.body[field] !== undefined) updated[field] = req.body[field];
  }
  tariffs.set(tariff.spotType, updated);
  res.json(updated);
});

// DELETE /api/billing/tariffs/EV
router.delete('/tariffs/:spotType', (req, res) => {
  const tariff = tariffs.get(spotTypeOf(req.params.spotType));
  if (!tariff) return notFound(res, req.params.spotType);
  tariffs.delete(tariff.spotType);
  res.status(204).end();
});

// POST /api/billing/quote  {"spotType":"EV","minutes":95}
router.post('/quote', (req, res) => {
  const minutes = req.body?.minutes;
  if (!Number.isInteger(minutes) || minutes < 0) {
    return res.status(400).json({ status: 400, error: 'Bad Request', message: 'minutes must be an integer >= 0' });
  }
  const tariff = tariffs.get(spotTypeOf(req.body?.spotType));
  if (!tariff) return notFound(res, req.body?.spotType);
  res.json(quote(tariff, minutes));
});

app.use('/api/billing', router);

app.get('/health', (req, res) => res.json({ status: 'UP' }));

app.get('/metrics', async (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
});

// Malformed JSON body -> 400 in JSON instead of an HTML page.
app.use((err, req, res, next) => {
  if (err.type === 'entity.parse.failed') {
    return res.status(400).json({ status: 400, error: 'Bad Request', message: 'Malformed JSON' });
  }
  console.error(err);
  res.status(500).json({ status: 500, error: 'Internal Server Error' });
});

app.use((req, res) => {
  res.status(404).json({ status: 404, error: 'Not Found', path: req.path });
});

module.exports = app;
