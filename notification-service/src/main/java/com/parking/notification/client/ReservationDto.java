package com.parking.notification.client;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The fields of the parking service's reservation JSON that this service uses; others are ignored.
 * {@code fallback} is true when the parking service could not be reached and the details are missing.
 */
public record ReservationDto(Integer id, String userId, Integer spotId, String spotNumber, Integer areaId,
                             String areaName, Instant startedAt, Instant endedAt, BigDecimal amount,
                             String currency, boolean active, long minutes, boolean fallback) {

    public static ReservationDto unavailable(int id) {
        return new ReservationDto(id, null, null, null, null, null, null, null, null, null, false, 0, true);
    }
}
