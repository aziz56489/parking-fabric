package com.parking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.parking.entity.Role;
import com.parking.entity.User;

public interface UserRepository extends JpaRepository<User , Integer>{
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);
}
