package com.parking.repository;

import com.parking.entity.ParkingArea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingAreaRepository extends JpaRepository<ParkingArea, Integer> {

    boolean existsByName(String name);
}
