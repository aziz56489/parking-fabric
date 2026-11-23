package com.parking.dto;

import com.parking.entity.NonPayingReport;
import com.parking.entity.ReportStatus;
import com.parking.entity.Reservation;
import com.parking.entity.User;

import java.math.BigDecimal;
import java.time.Instant;

/** Responses for the ticket counter (SCRUM-88). User details are added so the counter knows who to report. */
public final class CounterDtos {

    private CounterDtos() {
    }

    /** A reservation with its driver, and whether it was already reported as not paid. */
    public record CounterReservation(
            Integer id,
            Integer userId,
            String userName,
            String userEmail,
            String spotNumber,
            String areaName,
            Instant startedAt,
            Instant endedAt,
            long minutes,
            BigDecimal amount,
            String currency,
            boolean active,
            boolean reported) {

        public static CounterReservation of(Reservation reservation, Integer userId, User user, boolean reported) {
            return new CounterReservation(
                    reservation.getId(),
                    userId,
                    user != null ? user.getName() : null,
                    user != null ? user.getEmail() : null,
                    reservation.getSpotNumber(),
                    reservation.getAreaName(),
                    reservation.getStartedAt(),
                    reservation.getEndedAt(),
                    reservation.getMinutes(),
                    reservation.getAmount(),
                    reservation.getCurrency(),
                    reservation.isActive(),
                    reported);
        }
    }

    public record NonPayingReportResponse(
            Integer id,
            Integer reservationId,
            Integer userId,
            String userName,
            String userEmail,
            Integer ticketCounterId,
            String ticketCounterName,
            String spotNumber,
            String areaName,
            BigDecimal amount,
            String currency,
            String note,
            Instant reportedAt,
            ReportStatus status,
            boolean userBlocked,
            String banReason,
            String resolvedByName,
            Instant resolvedAt) {

        public static NonPayingReportResponse of(NonPayingReport report, User user, User counter, User resolver) {
            return new NonPayingReportResponse(
                    report.getId(),
                    report.getReservationId(),
                    report.getUserId(),
                    user != null ? user.getName() : null,
                    user != null ? user.getEmail() : null,
                    report.getTicketCounterId(),
                    counter != null ? counter.getName() : null,
                    report.getSpotNumber(),
                    report.getAreaName(),
                    report.getAmount(),
                    report.getCurrency(),
                    report.getNote(),
                    report.getReportedAt(),
                    report.getStatus(),
                    user != null && user.isBlocked(),
                    user != null ? user.getBanReason() : null,
                    resolver != null ? resolver.getName() : null,
                    report.getResolvedAt());
        }
    }
}
