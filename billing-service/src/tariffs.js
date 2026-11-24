'use strict';

/**
 * Tariffs per spot type, kept in memory (reset on restart).
 * Amounts are in TND with 3 decimals (millimes), like the parking service.
 */
const SPOT_TYPES = ['REGULAR', 'EV', 'HANDICAPPED', 'PERSONNEL'];

const tariffs = new Map([
  ['REGULAR', { spotType: 'REGULAR', pricePerHour: 1.5, currency: 'TND', freeMinutes: 0 }],
  ['EV', { spotType: 'EV', pricePerHour: 2.0, currency: 'TND', freeMinutes: 0 }],
  ['HANDICAPPED', { spotType: 'HANDICAPPED', pricePerHour: 0.5, currency: 'TND', freeMinutes: 30 }],
  ['PERSONNEL', { spotType: 'PERSONNEL', pricePerHour: 0, currency: 'TND', freeMinutes: 0 }],
]);

/** Returns a list of validation errors (empty when the body is valid). */
function validate(body, { partial = false } = {}) {
  const errors = [];
  if (body === null || typeof body !== 'object' || Array.isArray(body)) {
    return ['The body must be a JSON object'];
  }
  if (!partial || body.pricePerHour !== undefined) {
    if (typeof body.pricePerHour !== 'number' || !Number.isFinite(body.pricePerHour) || body.pricePerHour < 0) {
      errors.push('pricePerHour must be a number >= 0');
    }
  }
  if (body.freeMinutes !== undefined && (!Number.isInteger(body.freeMinutes) || body.freeMinutes < 0)) {
    errors.push('freeMinutes must be an integer >= 0');
  }
  if (body.currency !== undefined && (typeof body.currency !== 'string' || !/^[A-Z]{3}$/.test(body.currency))) {
    errors.push('currency must be a 3-letter code such as TND');
  }
  return errors;
}

/** Minutes after the free ones, billed per minute, rounded to the millime. */
function quote(tariff, minutes) {
  const billedMinutes = Math.max(0, minutes - tariff.freeMinutes);
  const amount = Math.round((tariff.pricePerHour * billedMinutes / 60) * 1000) / 1000;
  return {
    spotType: tariff.spotType,
    minutes,
    freeMinutes: tariff.freeMinutes,
    billedMinutes,
    pricePerHour: tariff.pricePerHour,
    amount,
    currency: tariff.currency,
  };
}

module.exports = { SPOT_TYPES, tariffs, validate, quote };
