package com.parking.repository;

import com.parking.entity.ParkingSpot;
import com.parking.entity.SpotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Integer> {

    /**
     * Changes the status in one SQL statement, only if it is still {@code from}.
     * Returns 0 when another request changed it first, so two users cannot reserve the same spot.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ParkingSpot s set s.status = :to where s.id = :id and s.status = :from")
    int changeStatus(@Param("id") int id, @Param("from") SpotStatus from, @Param("to") SpotStatus to);
}
