package com.parking.repository;

import com.parking.entity.NonPayingReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NonPayingReportRepository extends JpaRepository<NonPayingReport, Integer> {

    List<NonPayingReport> findAllByOrderByReportedAtDesc();

    boolean existsByReservationId(Integer reservationId);

    List<NonPayingReport> findByReservationIdIn(Collection<Integer> reservationIds);

    List<NonPayingReport> findByUserId(Integer userId);
}
