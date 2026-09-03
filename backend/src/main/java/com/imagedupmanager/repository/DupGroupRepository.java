package com.imagedupmanager.repository;

import com.imagedupmanager.domain.DupGroup;
import com.imagedupmanager.domain.DupGroupCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DupGroupRepository extends JpaRepository<DupGroup, Long> {

    List<DupGroup> findByScanId(Long scanId);

    List<DupGroup> findByScanIdAndCategory(Long scanId, DupGroupCategory category);
}
