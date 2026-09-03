package com.imagedupmanager.service;

import com.imagedupmanager.domain.DupGroup;
import com.imagedupmanager.domain.DupGroupCategory;
import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.Scan;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk JPQL updates used by the detection pass. Separate bean so that {@code @Transactional}
 * boundaries work from {@link DuplicateService}; avoids merging large detached graphs.
 */
@Component
public class DuplicateUpdater {

    private static final int BATCH_SIZE = 400;

    private final EntityManager entityManager;

    public DuplicateUpdater(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void clearGroupAssignments(Long scanId) {
        entityManager.createQuery(
                        "update ImageRecord r set r.group = null where r.scan.id = :scanId")
                .setParameter("scanId", scanId)
                .executeUpdate();
    }

    @Transactional
    public void updateShaAndAttributes(Long id, String sha256, long sizeBytes,
                                       LocalDateTime lastModifiedUtc) {
        entityManager.createQuery(
                        "update ImageRecord r set r.sha256 = :sha256, "
                                + "r.sizeBytes = :sizeBytes, r.lastModified = :lastModified "
                                + "where r.id = :id")
                .setParameter("sha256", sha256)
                .setParameter("sizeBytes", sizeBytes)
                .setParameter("lastModified", lastModifiedUtc)
                .setParameter("id", id)
                .executeUpdate();
    }

    @Transactional
    public void updatePerceptual(Long id, Long phash, int width, int height,
                                 Integer exifOrientation) {
        entityManager.createQuery(
                        "update ImageRecord r set r.phash = :phash, r.width = :width, "
                                + "r.height = :height, r.exifOrientation = :orientation "
                                + "where r.id = :id")
                .setParameter("phash", phash)
                .setParameter("width", width)
                .setParameter("height", height)
                .setParameter("orientation", exifOrientation)
                .setParameter("id", id)
                .executeUpdate();
    }

    @Transactional
    public void deleteGroupsForScan(Long scanId) {
        entityManager.createQuery("delete from DupGroup g where g.scan.id = :scanId")
                .setParameter("scanId", scanId)
                .executeUpdate();
    }

    @Transactional
    public Long saveGroup(Long scanId, DupGroupCategory category, Long recommendedImageId,
                          int memberCount, long reclaimableBytes) {
        Scan scanProxy = entityManager.getReference(Scan.class, scanId);
        DupGroup group = new DupGroup(scanProxy, category);
        group.setRecommendedImageId(recommendedImageId);
        group.setMemberCount(memberCount);
        group.setReclaimableBytes(reclaimableBytes);
        entityManager.persist(group);
        return group.getId();
    }

    @Transactional
    public void assignGroup(Long groupId, List<Long> imageIds) {
        Object groupReference = entityManager.getReference(DupGroup.class, groupId);
        List<Long> pending = new ArrayList<>(imageIds);
        while (!pending.isEmpty()) {
            List<Long> chunk = pending.subList(0, Math.min(BATCH_SIZE, pending.size()));
            List<Long> copy = new ArrayList<>(chunk);
            chunk.clear();
            entityManager.createQuery(
                            "update ImageRecord r set r.group = :group where r.id in :ids")
                    .setParameter("group", groupReference)
                    .setParameter("ids", copy)
                    .executeUpdate();
        }
    }
}
