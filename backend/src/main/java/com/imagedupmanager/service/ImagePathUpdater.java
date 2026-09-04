package com.imagedupmanager.service;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.ImageStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Small transactional helper to update an image record after a file operation, avoiding
 * detached-entity merge issues.
 */
@Component
public class ImagePathUpdater {

    private final EntityManager entityManager;

    public ImagePathUpdater(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void updatePathAndName(Long imageId, String absolutePath, String fileName) {
        entityManager.createQuery(
                        "update ImageRecord r set r.absolutePath = :path, r.name = :name "
                                + "where r.id = :id")
                .setParameter("path", absolutePath)
                .setParameter("name", fileName)
                .setParameter("id", imageId)
                .executeUpdate();
    }

    @Transactional
    public void updateStatus(Long imageId, ImageStatus status) {
        entityManager.createQuery(
                        "update ImageRecord r set r.status = :status where r.id = :id")
                .setParameter("status", status)
                .setParameter("id", imageId)
                .executeUpdate();
    }
}
