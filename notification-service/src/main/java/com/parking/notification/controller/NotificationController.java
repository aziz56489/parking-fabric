package com.parking.notification.controller;

import com.parking.notification.client.ReservationDto;
import com.parking.notification.model.Notification;
import com.parking.notification.service.NotificationService;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** All endpoints require a Keycloak access token (see SecurityConfig). */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Notifications created from Kafka events, newest first; filtered by user when userId is given. */
    @GetMapping
    public List<Notification> getNotifications(@RequestParam(required = false) String userId) {
        return notificationService.findAll(userId);
    }

    /**
     * Reservation read from the parking service with Feign (synchronous inter-service call).
     * When the parking service is down, the circuit breaker's fallback answers with "fallback": true.
     */
    @GetMapping("/reservations/{id}")
    public ReservationDto getReservation(@PathVariable int id) {
        try {
            return notificationService.getReservation(id);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
    }

    /** A user's reservations read from the parking service with Feign (empty list as fallback). */
    @GetMapping("/users/{userId}/reservations")
    public List<ReservationDto> getReservations(@PathVariable String userId) {
        return notificationService.getReservations(userId);
    }
}
