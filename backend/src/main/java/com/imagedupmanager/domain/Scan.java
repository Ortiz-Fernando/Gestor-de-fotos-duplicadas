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
 * A folder-scanning run (one analysis of a root path).
 */
@Entity
@Table(name = "scan", indexes = {
        @Index(name = "idx_scan_status", columnList = "status")
})
public class Scan {

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

    @Column(name = "root_path", nullable = false, length = 1024)
    private String rootPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanStatus status = ScanStatus.RUNNING;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "file_count", nullable = false)
    private int fileCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    /** Snapshot of the scan options used (JSON). Fully used by ScanService (Fase 4+). */
    @Column(name = "options", columnDefinition = "TEXT")
    private String options;

    protected Scan() {
        // Required by JPA.
    }

    public Scan(String rootPath, LocalDateTime startedAt) {
        this.rootPath = rootPath;
        this.startedAt = startedAt;
    }

    public Long getId() {
        return id;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public void setStatus(ScanStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }
}
