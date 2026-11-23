package com.parking.controller;

import com.parking.dto.CounterDtos.CounterReservation;
import com.parking.dto.CounterDtos.NonPayingReportResponse;
import com.parking.dto.ParkingRequests.NonPayingReportRequest;
import com.parking.service.TicketCounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** SCRUM-88: ticket counter endpoints. Only TICKET_COUNTER and ADMIN users get here (see SecurityConfig). */
@RestController
@RequestMapping("/api/counter")
@RequiredArgsConstructor
public class TicketCounterController {

    private final TicketCounterService ticketCounterService;

    @GetMapping("/reservations")
    public List<CounterReservation> reservations() {
        return ticketCounterService.recentReservations();
    }

    @GetMapping("/non-paying-reports")
    public List<NonPayingReportResponse> reports() {
        return ticketCounterService.reports();
    }

    @PostMapping("/non-paying-reports")
    @ResponseStatus(HttpStatus.CREATED)
    public NonPayingReportResponse report(@RequestBody NonPayingReportRequest request, Authentication authentication) {
        return ticketCounterService.report((Integer) authentication.getPrincipal(), request);
    }
}
