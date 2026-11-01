package com.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String number;

    @Enumerated(EnumType.STRING)
    private SpotType type;

    @Enumerated(EnumType.STRING)
    private SpotStatus status = SpotStatus.AVAILABLE;

    @ManyToOne
    @JsonIgnore
    private ParkingArea parkingArea;

    public ParkingSpot(String number, SpotType type, ParkingArea parkingArea) {
        this.number = number;
        this.type = type;
        this.parkingArea = parkingArea;
    }

    public boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }

    public void updateStatus(SpotStatus status) {
        this.status = status;
    }

    public void updateType(SpotType type) {
        this.type = type;
    }
}
