package com.parking.controller;

import com.parking.dto.ParkingRequests.IssueRequest;
import com.parking.dto.ParkingRequests.ReservationRequest;
import com.parking.entity.IssueReport;
import com.parking.entity.Reservation;
import com.parking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/reservations")
    public ResponseEntity<Reservation> reserve(@RequestBody ReservationRequest request) {
        Reservation reservation = reservationService.reserve(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(reservation.getId())
                .toUri();
        return ResponseEntity.created(location).body(reservation);
    }

    @GetMapping("/reservations")
    public List<Reservation> getReservations(@RequestParam String userId) {
        return reservationService.getReservations(userId);
    }

    @GetMapping("/reservations/{id}")
    public Reservation getReservation(@PathVariable int id) {
        return reservationService.getReservation(id);
    }

    @PostMapping("/reservations/{id}/exit")
    public Reservation exit(@PathVariable int id) {
        return reservationService.exit(id);
    }

    @PostMapping("/reservations/{id}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueReport reportIssue(@PathVariable int id, @RequestBody IssueRequest request) {
        return reservationService.reportIssue(id, request);
    }

    @GetMapping("/issues")
    public List<IssueReport> getIssues() {
        return reservationService.getIssues();
    }
}
