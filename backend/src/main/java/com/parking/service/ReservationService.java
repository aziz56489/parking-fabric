package com.parking.service;

import com.parking.dto.ParkingRequests.IssueRequest;
import com.parking.dto.ParkingRequests.ReservationRequest;
import com.parking.entity.IssueReport;
import com.parking.entity.ParkingSpot;
import com.parking.entity.Reservation;
import com.parking.entity.SpotStatus;
import com.parking.entity.User;
import com.parking.event.ReservationEvent;
import com.parking.repository.IssueReportRepository;
import com.parking.repository.ParkingSpotRepository;
import com.parking.repository.ReservationRepository;
import com.parking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final IssueReportRepository issueReportRepository;
    private final UserRepository userRepository;
    private final BigDecimal pricePerHour;
    private final String currency;
    private final ApplicationEventPublisher events;

    public ReservationService(ReservationRepository reservationRepository,
                              ParkingSpotRepository parkingSpotRepository,
                              IssueReportRepository issueReportRepository,
                              UserRepository userRepository,
                              @Value("${parking.price-per-hour}") BigDecimal pricePerHour,
                              @Value("${parking.currency}") String currency,
                              ApplicationEventPublisher events) {
        this.reservationRepository = reservationRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.issueReportRepository = issueReportRepository;
        this.userRepository = userRepository;
        this.pricePerHour = pricePerHour;
        this.currency = currency;
        this.events = events;
    }

    @Transactional
    public Reservation reserve(ReservationRequest request) {
        if (request == null || request.spotId() == null || request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "spotId and userId are required");
        }
        // A user banned after a non paying report may still have an open session on the phone.
        if (isBlocked(request.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is blocked. Please contact support");
        }
        if (reservationRepository.existsByUserIdAndEndedAtIsNull(request.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active reservation");
        }
        ParkingSpot spot = parkingSpotRepository.findById(request.spotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking spot not found"));
        if (parkingSpotRepository.changeStatus(spot.getId(), SpotStatus.AVAILABLE, SpotStatus.RESERVED) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Spot " + spot.getNumber() + " is not available");
        }
        Reservation reservation = reservationRepository.save(new Reservation(request.userId(), spot, priceOf(spot), currency));
        // Sent to Kafka after commit by ReservationEventPublisher.
        events.publishEvent(ReservationEvent.of(ReservationEvent.CREATED, reservation));
        return reservation;
    }

    /** The price the admin set for this spot in the layout editor, or the default price. */
    private BigDecimal priceOf(ParkingSpot spot) {
        return spot.getParkingArea() == null
                ? pricePerHour
                : spot.getParkingArea().findSpotPrice(spot.getNumber()).orElse(pricePerHour);
    }

    private boolean isBlocked(String userId) {
        try {
            return userRepository.findById(Integer.valueOf(userId.trim())).map(User::isBlocked).orElse(false);
        } catch (NumberFormatException e) {
            return false; // old app builds used a random id, not an account
        }
    }

    public Reservation getReservation(int id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    public List<Reservation> getReservations(String userId) {
        return reservationRepository.findByUserIdOrderByStartedAtDesc(userId);
    }

    /** Ends the reservation, frees the spot and returns the receipt. Calling it twice returns the same receipt. */
    @Transactional
    public Reservation exit(int id) {
        Reservation reservation = getReservation(id);
        if (reservation.isActive()) {
            reservation.end();
            events.publishEvent(ReservationEvent.of(ReservationEvent.ENDED, reservation));
            // An admin may have freed the spot and someone else reserved it since: leave it to them.
            if (!reservationRepository.existsBySpotIdAndEndedAtIsNull(reservation.getSpotId())) {
                parkingSpotRepository.changeStatus(reservation.getSpotId(), SpotStatus.RESERVED, SpotStatus.AVAILABLE);
            }
        }
        return reservation;
    }

    public IssueReport reportIssue(int reservationId, IssueRequest request) {
        if (request == null || request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The issue type is required");
        }
        String description = request.description() == null ? "" : request.description().trim();
        if (description.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The description is limited to 1000 characters");
        }
        Reservation reservation = getReservation(reservationId);
        return issueReportRepository.save(new IssueReport(reservation, request.type(), description));
    }

    public List<IssueReport> getIssues() {
        return issueReportRepository.findAllByOrderByReportedAtDesc();
    }
}
