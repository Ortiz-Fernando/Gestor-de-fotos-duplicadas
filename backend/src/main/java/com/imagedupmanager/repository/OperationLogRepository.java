package com.imagedupmanager.repository;

import com.imagedupmanager.domain.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findAllByOrderByOperationTimeDesc();

    List<OperationLog> findByImageId(Long imageId);
}
