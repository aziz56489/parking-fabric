package com.parking.notification.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The Feign client protected by Resilience4j (instance "parkingService", thresholds in application.properties):
 * - Retry: up to 3 attempts with exponential backoff (500 ms, 1 s) for transient failures;
 * - CircuitBreaker: opens when 50 % of the last 10 calls fail or are slower than 2 s, stays open 15 s,
 *   then lets 3 trial calls through (half-open) before closing again;
 * - Fallback: when the parking service is down or the circuit is open, a degraded answer is returned
 *   instead of an error. A 404 is a normal answer: it is not counted as a failure and is passed on.
 */
@Component
public class ResilientParkingClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientParkingClient.class);
    private static final String PARKING_SERVICE = "parkingService";

    private final ParkingServiceClient parkingClient;

    public ResilientParkingClient(ParkingServiceClient parkingClient) {
        this.parkingClient = parkingClient;
    }

    @Retry(name = PARKING_SERVICE, fallbackMethod = "reservationFallback")
    @CircuitBreaker(name = PARKING_SERVICE)
    public ReservationDto getReservation(int id) {
        return parkingClient.getReservation(id);
    }

    @Retry(name = PARKING_SERVICE, fallbackMethod = "reservationsFallback")
    @CircuitBreaker(name = PARKING_SERVICE)
    public List<ReservationDto> getReservations(String userId) {
        return parkingClient.getReservations(userId);
    }

    @SuppressWarnings("unused")
    private ReservationDto reservationFallback(int id, Throwable error) {
        rethrowNotFound(error);
        log.warn("Fallback for reservation {}: {}", id, error.toString());
        return ReservationDto.unavailable(id);
    }

    @SuppressWarnings("unused")
    private List<ReservationDto> reservationsFallback(String userId, Throwable error) {
        rethrowNotFound(error);
        log.warn("Fallback for the reservations of user {}: {}", userId, error.toString());
        return List.of();
    }

    private static void rethrowNotFound(Throwable error) {
        if (error instanceof FeignException.NotFound notFound) {
            throw notFound;
        }
    }
}
