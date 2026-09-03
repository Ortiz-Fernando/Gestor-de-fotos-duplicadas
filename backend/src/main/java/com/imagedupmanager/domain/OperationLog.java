package com.imagedupmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import java.time.LocalDateTime;

/**
 * History entry of a user operation (rename, move, trash, undo).
 *
 * <p>The affected image is referenced by plain id to keep the log independent of the
 * image lifecycle. Reversibility is tracked per operation (ADR: undo only when possible).
 */
@Entity
@Table(name = "operation_log", indexes = {
        @Index(name = "idx_oplog_time", columnList = "operation_time"),
        @Index(name = "idx_oplog_image", columnList = "image_id")
})
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "id_generator")
    @TableGenerator(
            name = "id_generator",
            table = "hibernate_sequences",
            pkColumnName = "sequence_name",
            valueColumnName = "next_val",
            pkColumnValue = "imagedupmanager_id",
            allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private OperationType type;

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "source_path", length = 1024)
    private String sourcePath;

    @Column(name = "destination_path", length = 1024)
    private String destinationPath;

    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;

    @Column(name = "reversible", nullable = false)
    private boolean reversible = true;

    @Column(name = "undone_at")
    private LocalDateTime undoneAt;

    protected OperationLog() {
        // Required by JPA.
    }

    public OperationLog(OperationType type, Long imageId, String sourcePath,
                        String destinationPath, LocalDateTime operationTime) {
        this.type = type;
        this.imageId = imageId;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.operationTime = operationTime;
    }

    public Long getId() {
        return id;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    public boolean isReversible() {
        return reversible;
    }

    public void setReversible(boolean reversible) {
        this.reversible = reversible;
    }

    public LocalDateTime getUndoneAt() {
        return undoneAt;
    }

    public void setUndoneAt(LocalDateTime undoneAt) {
        this.undoneAt = undoneAt;
    }
}
