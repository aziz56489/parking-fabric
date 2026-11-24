package com.parking.notification.service;

import com.parking.notification.client.ReservationDto;
import com.parking.notification.client.ResilientParkingClient;
import com.parking.notification.model.Notification;
import com.parking.notification.model.ReservationEvent;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Turns reservation events into user notifications.
 * Notifications are kept in memory (newest first, at most MAX_SIZE): enough for the demo, lost on restart.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_SIZE = 500;

    private final ResilientParkingClient parkingClient;
    private final Deque<Notification> notifications = new ConcurrentLinkedDeque<>();
    private final AtomicLong ids = new AtomicLong();

    public NotificationService(ResilientParkingClient parkingClient) {
        this.parkingClient = parkingClient;
    }

    public Notification handle(ReservationEvent event) {
        String message = buildMessage(event);
        Notification notification = new Notification(ids.incrementAndGet(), event.userId(), event.reservationId(),
                event.type(), message, Instant.now());
        notifications.addFirst(notification);
        while (notifications.size() > MAX_SIZE) {
            notifications.pollLast();
        }
        log.info("Notification for user {}: {}", event.userId(), message);
        return notification;
    }

    public List<Notification> findAll(String userId) {
        return notifications.stream()
                .filter(n -> userId == null || userId.equals(n.userId()))
                .toList();
    }

    /** Synchronous call (Feign, protected by the circuit breaker) to the parking service. */
    public ReservationDto getReservation(int reservationId) {
        return parkingClient.getReservation(reservationId);
    }

    public List<ReservationDto> getReservations(String userId) {
        return parkingClient.getReservations(userId);
    }

    private String buildMessage(ReservationEvent event) {
        ReservationDto reservation;
        try {
            reservation = parkingClient.getReservation(event.reservationId());
        } catch (FeignException e) {
            log.warn("Reservation {} not found in the parking service: {}", event.reservationId(), e.getMessage());
            reservation = ReservationDto.unavailable(event.reservationId());
        }
        if (reservation.fallback()) {
            // Degraded message, built only from the data carried by the Kafka event.
            return switch (event.type()) {
                case "RESERVATION_CREATED" -> "Your reservation #" + event.reservationId() + " is confirmed.";
                case "RESERVATION_ENDED" -> "Your reservation #" + event.reservationId() + " has ended.";
                default -> "Reservation #" + event.reservationId() + ": " + event.type();
            };
        }
        return switch (event.type()) {
            case "RESERVATION_CREATED" -> "Spot " + reservation.spotNumber() + " in " + reservation.areaName()
                    + " is reserved for you (reservation #" + reservation.id() + ").";
            case "RESERVATION_ENDED" -> "Thank you! You stayed " + reservation.minutes() + " min on spot "
                    + reservation.spotNumber() + ". Amount due: " + reservation.amount() + " "
                    + reservation.currency() + ".";
            default -> "Reservation #" + reservation.id() + ": " + event.type();
        };
    }
}
