package com.parking.notification.model;

import java.time.Instant;

public record Notification(long id, String userId, Integer reservationId, String type, String message,
                           Instant createdAt) {
}
