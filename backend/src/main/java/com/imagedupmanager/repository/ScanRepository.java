package com.imagedupmanager.repository;

import com.imagedupmanager.domain.Scan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScanRepository extends JpaRepository<Scan, Long> {

    List<Scan> findAllByOrderByStartedAtDesc();

    Optional<Scan> findTopByOrderByIdDesc();
}
