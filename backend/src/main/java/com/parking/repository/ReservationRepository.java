package com.parking.repository;

import com.parking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    boolean existsByUserIdAndEndedAtIsNull(String userId);

    boolean existsBySpotIdAndEndedAtIsNull(Integer spotId);

    List<Reservation> findByUserIdOrderByStartedAtDesc(String userId);

    /** The most recent reservations, for the ticket counter. */
    List<Reservation> findTop100ByOrderByStartedAtDesc();

    /** Paid stays (earnings are counted on the exit date). */
    List<Reservation> findByEndedAtGreaterThanEqual(java.time.Instant from);

    /** Entries (a reservation starts when the driver enters). */
    List<Reservation> findByStartedAtGreaterThanEqual(java.time.Instant from);
}
