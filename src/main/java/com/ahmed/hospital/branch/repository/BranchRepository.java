package com.ahmed.hospital.branch.repository;

import com.ahmed.hospital.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Branch> findByNameIgnoreCase(String name);

    Optional<Branch> findByEmailIgnoreCase(String email);
}