package com.parking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@NoArgsConstructor
public class IssueReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer reservationId;

    private Integer spotId;

    private String spotNumber;

    private String areaName;

    @Enumerated(EnumType.STRING)
    private IssueType type;

    @Column(length = 1000)
    private String description;

    private Instant reportedAt;

    public IssueReport(Reservation reservation, IssueType type, String description) {
        this.reservationId = reservation.getId();
        this.spotId = reservation.getSpotId();
        this.spotNumber = reservation.getSpotNumber();
        this.areaName = reservation.getAreaName();
        this.type = type;
        this.description = description;
        this.reportedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
