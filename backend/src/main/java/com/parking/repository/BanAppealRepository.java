package com.parking.repository;

import com.parking.entity.AppealStatus;
import com.parking.entity.BanAppeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BanAppealRepository extends JpaRepository<BanAppeal, Integer> {

    List<BanAppeal> findAllByOrderByCreatedAtDesc();

    Optional<BanAppeal> findFirstByUserIdOrderByCreatedAtDesc(Integer userId);

    boolean existsByUserIdAndStatus(Integer userId, AppealStatus status);

    List<BanAppeal> findByUserIdAndStatus(Integer userId, AppealStatus status);
}
