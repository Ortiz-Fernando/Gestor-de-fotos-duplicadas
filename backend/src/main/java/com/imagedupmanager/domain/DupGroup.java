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

/**
 * A group of images detected as exact or visually possible duplicates.
 *
 * <p>Members are linked back to the group through {@link ImageRecord#getGroup()}. The
 * recommended file to keep is stored by id in {@link #recommendedImageId} (plain column,
 * no DB-level FK, to keep group assignment decoupled from file lifecycle).
 */
@Entity
@Table(name = "dup_group", indexes = {
        @Index(name = "idx_dupgroup_scan", columnList = "scan_id")
})
public class DupGroup {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private DupGroupCategory category;

    @Column(name = "recommended_image_id")
    private Long recommendedImageId;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @Column(name = "reclaimable_bytes", nullable = false)
    private long reclaimableBytes;

    protected DupGroup() {
        // Required by JPA.
    }

    public DupGroup(Scan scan, DupGroupCategory category) {
        this.scan = scan;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public Scan getScan() {
        return scan;
    }

    public DupGroupCategory getCategory() {
        return category;
    }

    public void setCategory(DupGroupCategory category) {
        this.category = category;
    }

    public Long getRecommendedImageId() {
        return recommendedImageId;
    }

    public void setRecommendedImageId(Long recommendedImageId) {
        this.recommendedImageId = recommendedImageId;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public long getReclaimableBytes() {
        return reclaimableBytes;
    }

    public void setReclaimableBytes(long reclaimableBytes) {
        this.reclaimableBytes = reclaimableBytes;
    }
}
