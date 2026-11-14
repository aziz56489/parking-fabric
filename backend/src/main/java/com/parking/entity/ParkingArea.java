package com.parking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name="parking_area" , schema= "public")
public class ParkingArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String name;

    // PostgreSQL text avoids Hibernate's OID-based large-object handling for @Lob.
    @Column(columnDefinition = "text")
    private String layout;

    private int capacity;

    @OneToMany(mappedBy = "parkingArea", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkingSpot> spots = new ArrayList<>();

    public double getOccupancyRate() {
        if (spots.isEmpty()) {
            return 0;
        }
        long occupied = spots.stream()
                .filter(spot -> spot.getStatus() == SpotStatus.OCCUPIED)
                .count();
        return (double) occupied / spots.size();
    }
}
