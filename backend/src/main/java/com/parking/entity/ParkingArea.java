package com.parking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /** Hourly price of each spot, set by the admin in the layout editor. Spots not listed use the default price. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "spot_pricing", columnDefinition = "jsonb")
    private List<SpotPrice> spotPricing = new ArrayList<>();

    @OneToMany(mappedBy = "parkingArea", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkingSpot> spots = new ArrayList<>();

    /**
     * Share of usable spots that are taken. A reservation puts a spot in RESERVED
     * (nothing currently sets OCCUPIED), so both count as taken. Spots marked
     * NOT_AVAILABLE can't be used, so they are left out of the total.
     */
    public double getOccupancyRate() {
        long usable = spots.stream()
                .filter(spot -> spot.getStatus() != SpotStatus.NOT_AVAILABLE)
                .count();
        if (usable == 0) {
            return 0;
        }
        long taken = spots.stream()
                .filter(spot -> spot.getStatus() == SpotStatus.RESERVED || spot.getStatus() == SpotStatus.OCCUPIED)
                .count();
        return (double) taken / usable;
    }

    /** The price the admin set for this spot, if any. */
    public Optional<BigDecimal> findSpotPrice(String spotNumber) {
        if (spotPricing == null) {
            return Optional.empty();
        }
        return spotPricing.stream()
                .filter(price -> price.spotNumber().equals(spotNumber) && price.pricePerHour() != null)
                .map(SpotPrice::pricePerHour)
                .findFirst();
    }
}
