package com.parking.repository;

import com.parking.entity.IssueReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueReportRepository extends JpaRepository<IssueReport, Integer> {

    List<IssueReport> findAllByOrderByReportedAtDesc();
}
