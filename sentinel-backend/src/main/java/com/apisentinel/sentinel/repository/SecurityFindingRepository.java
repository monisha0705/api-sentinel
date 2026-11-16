package com.apisentinel.sentinel.repository;

import com.apisentinel.sentinel.finding.SecurityFindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecurityFindingRepository
        extends JpaRepository<SecurityFindingEntity, Long> {

    Optional<SecurityFindingEntity> findByFindingId(String findingId);

    List<SecurityFindingEntity> findAllByOrderByDetectedAtDesc();

    List<SecurityFindingEntity> findBySeverityOrderByDetectedAtDesc(
            String severity
    );
}

