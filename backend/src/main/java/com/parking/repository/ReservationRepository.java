package com.parking.repository;

import com.parking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    boolean existsByUserIdAndEndedAtIsNull(String userId);

    boolean existsBySpotIdAndEndedAtIsNull(Integer spotId);

    List<Reservation> findByUserIdOrderByStartedAtDesc(String userId);
}
