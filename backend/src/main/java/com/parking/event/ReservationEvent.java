package com.parking.event;

import com.parking.entity.Reservation;

import java.time.Instant;

/**
 * Message published on Kafka when a reservation starts or ends.
 * It only carries identifiers: consumers fetch the full reservation from the parking service (Feign).
 */
public record ReservationEvent(String type, Integer reservationId, String userId, Integer spotId, Instant occurredAt) {

    public static final String CREATED = "RESERVATION_CREATED";
    public static final String ENDED = "RESERVATION_ENDED";

    public static ReservationEvent of(String type, Reservation reservation) {
        return new ReservationEvent(type, reservation.getId(), reservation.getUserId(), reservation.getSpotId(),
                Instant.now());
    }
}
