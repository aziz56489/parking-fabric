package com.parking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * SCRUM-88: a ticket counter reports a user who left without paying.
 * The reservation details are copied so the report stays the same if the layout changes later.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "non_paying_report", schema = "public")
public class NonPayingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The TICKET_COUNTER (or ADMIN) user who filed the report. */
    @Column(nullable = false)
    private Integer ticketCounterId;

    /** The reported user. */
    @Column(nullable = false)
    private Integer userId;

    @Column(unique = true)
    private Integer reservationId;

    private String spotNumber;

    private String areaName;

    @Column(precision = 10, scale = 3)
    private BigDecimal amount;

    private String currency;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private Instant reportedAt;

    /** Null for reports filed before admins could review them; read as PENDING. */
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.PENDING;

    /** The admin who banned the user or dismissed the report, and when. */
    private Integer resolvedById;

    private Instant resolvedAt;

    public ReportStatus getStatus() {
        return status != null ? status : ReportStatus.PENDING;
    }

    public void resolve(ReportStatus decision, int adminId) {
        this.status = decision;
        this.resolvedById = adminId;
        this.resolvedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public NonPayingReport(int ticketCounterId, int userId, Reservation reservation, String note) {
        this.ticketCounterId = ticketCounterId;
        this.userId = userId;
        this.reservationId = reservation.getId();
        this.spotNumber = reservation.getSpotNumber();
        this.areaName = reservation.getAreaName();
        this.amount = reservation.getAmount();
        this.currency = reservation.getCurrency();
        this.note = note;
        this.reportedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
