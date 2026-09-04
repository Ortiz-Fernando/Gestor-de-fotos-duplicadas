package com.imagedupmanager.repository;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.ImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageRecordRepository extends JpaRepository<ImageRecord, Long> {

    List<ImageRecord> findByScanId(Long scanId);

    List<ImageRecord> findByScanIdOrderByAbsolutePathAsc(Long scanId);

    List<ImageRecord> findByScanIdAndStatusOrderByAbsolutePathAsc(Long scanId, ImageStatus status);

    List<ImageRecord> findByScanIdAndSha256(Long scanId, String sha256);

    boolean existsByScanIdAndAbsolutePath(Long scanId, String absolutePath);

    Optional<ImageRecord> findByScanIdAndAbsolutePath(Long scanId, String absolutePath);

    List<ImageRecord> findByGroupId(Long groupId);

    List<ImageRecord> findByGroupIdAndStatus(Long groupId, ImageStatus status);

    long countByScanId(Long scanId);

    long countByScanIdAndSha256IsNotNull(Long scanId);
}
