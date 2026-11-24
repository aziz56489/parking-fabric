package com.parking.notification.model;

import java.time.Instant;

/** Kafka message published by the parking service (same JSON as com.parking.event.ReservationEvent). */
public record ReservationEvent(String type, Integer reservationId, String userId, Integer spotId, Instant occurredAt) {
}
