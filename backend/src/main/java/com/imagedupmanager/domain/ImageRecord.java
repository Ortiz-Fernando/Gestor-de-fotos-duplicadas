package com.imagedupmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Metadata record of an image file discovered during a scan.
 *
 * <p>SHA-256 and pHash are nullable: they are filled by the detection stages that run
 * after enumeration (Fases 5-6). HEIC/HEIF/RAW files never get a pHash (ADR D4).
 */
@Entity
@Table(name = "image_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_image_scan_path", columnNames = {"scan_id", "absolute_path"})
        },
        indexes = {
                @Index(name = "idx_image_scan", columnList = "scan_id"),
                @Index(name = "idx_image_sha256", columnList = "sha256"),
                @Index(name = "idx_image_scan_size", columnList = "scan_id, size_bytes"),
                @Index(name = "idx_image_scan_phash", columnList = "scan_id, phash"),
                @Index(name = "idx_image_scan_group", columnList = "scan_id, group_id")
        })
public class ImageRecord {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @Column(name = "absolute_path", nullable = false, length = 1024)
    private String absolutePath;

    @Column(name = "file_name", nullable = false, length = 512)
    private String name;

    @Column(name = "folder", length = 1024)
    private String folder;

    @Column(name = "extension", length = 16)
    private String extension;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    @Column(name = "sha256", length = 64)
    private String sha256;

    /** Perceptual hash (64 bits) as an unsigned long value, or null when not analysable. */
    @Column(name = "phash")
    private Long phash;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "exif_orientation")
    private Integer exifOrientation;

    /** True when the image format is supported for visual analysis (ADR D4). */
    @Column(name = "analysable", nullable = false)
    private boolean analysable = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImageStatus status = ImageStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private DupGroup group;

    protected ImageRecord() {
        // Required by JPA.
    }

    public ImageRecord(Scan scan, String absolutePath, String name, String folder,
                       String extension, long sizeBytes, LocalDateTime lastModified) {
        this.scan = scan;
        this.absolutePath = absolutePath;
        this.name = name;
        this.folder = folder;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.lastModified = lastModified;
    }

    public Long getId() {
        return id;
    }

    public Scan getScan() {
        return scan;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Long getPhash() {
        return phash;
    }

    public void setPhash(Long phash) {
        this.phash = phash;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getExifOrientation() {
        return exifOrientation;
    }

    public void setExifOrientation(Integer exifOrientation) {
        this.exifOrientation = exifOrientation;
    }

    public boolean isAnalysable() {
        return analysable;
    }

    public void setAnalysable(boolean analysable) {
        this.analysable = analysable;
    }

    public ImageStatus getStatus() {
        return status;
    }

    public void setStatus(ImageStatus status) {
        this.status = status;
    }

    public DupGroup getGroup() {
        return group;
    }

    public void setGroup(DupGroup group) {
        this.group = group;
    }
}
