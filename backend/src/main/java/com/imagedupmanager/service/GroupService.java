package com.imagedupmanager.service;

import com.imagedupmanager.domain.DupGroup;
import com.imagedupmanager.domain.DupGroupCategory;
import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.ImageStatus;
import com.imagedupmanager.repository.DupGroupRepository;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.web.dto.ApiDtos.GroupDetail;
import com.imagedupmanager.web.dto.ApiDtos.GroupSummary;
import com.imagedupmanager.web.dto.ApiDtos.Image;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Keeps duplicate groups coherent with the image lifecycle.
 *
 * <p>Only {@link ImageStatus#ACTIVE} images count as group members. When an image is sent
 * to the trash it is removed from its group; counters and the recommended image are
 * recomputed, and a group left with fewer than two active members is deleted (it is no
 * longer a duplicate). Read endpoints ignore non-active members, so previously
 * inconsistent rows self-heal on access.
 */
@Service
public class GroupService {

    private final DupGroupRepository dupGroupRepository;
    private final ImageRecordRepository imageRecordRepository;
    private final EntityManager entityManager;

    public GroupService(DupGroupRepository dupGroupRepository,
                        ImageRecordRepository imageRecordRepository,
                        EntityManager entityManager) {
        this.dupGroupRepository = dupGroupRepository;
        this.imageRecordRepository = imageRecordRepository;
        this.entityManager = entityManager;
    }

    /** Marks the image as {@code IN_TRASH}, unassigns it from its group and refreshes the
     *  group (or removes it when fewer than two active members remain). */
    @Transactional
    public void markTrashedAndRefresh(Long imageId) {
        ImageRecord record = imageRecordRepository.findById(imageId)
                .orElseThrow(() -> new OperationException("No se ha encontrado la imagen solicitada."));
        DupGroup group = record.getGroup();
        Long groupId = (group == null) ? null : group.getId();

        entityManager.createQuery(
                        "update ImageRecord r set r.status = :status, r.group = null where r.id = :id")
                .setParameter("status", ImageStatus.IN_TRASH)
                .setParameter("id", imageId)
                .executeUpdate();

        if (groupId != null) {
            refreshOrRemove(groupId);
        }
    }

    private void refreshOrRemove(Long groupId) {
        List<ImageRecord> activeMembers =
                imageRecordRepository.findByGroupIdAndStatus(groupId, ImageStatus.ACTIVE);
        if (activeMembers.size() < 2) {
            removeGroupSilently(groupId);
            return;
        }
        DupGroup group = dupGroupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return;
        }
        ImageRecord recommended = ImageRecommendation.recommend(activeMembers);
        group.setMemberCount(activeMembers.size());
        group.setRecommendedImageId(recommended.getId());
        group.setReclaimableBytes(ImageRecommendation.reclaimableBytes(activeMembers, recommended));
        dupGroupRepository.save(group);
    }

    /** Summaries computed only from ACTIVE members; stale groups self-heal on access. */
    @Transactional
    public List<GroupSummary> summariesForScan(Long scanId, String category) {
        List<DupGroup> groups = (category == null || category.isBlank())
                ? dupGroupRepository.findByScanId(scanId)
                : dupGroupRepository.findByScanIdAndCategory(scanId,
                        DupGroupCategory.valueOf(category));
        List<GroupSummary> summaries = new ArrayList<>();
        for (DupGroup group : groups) {
            List<ImageRecord> activeMembers =
                    imageRecordRepository.findByGroupIdAndStatus(group.getId(), ImageStatus.ACTIVE);
            if (activeMembers.size() < 2) {
                removeGroupSilently(group.getId());
                continue;
            }
            ImageRecord recommended = ImageRecommendation.recommend(activeMembers);
            summaries.add(new GroupSummary(group.getId(), scanId, group.getCategory().name(),
                    recommended.getId(), activeMembers.size(),
                    ImageRecommendation.reclaimableBytes(activeMembers, recommended)));
        }
        return summaries;
    }

    /** Group detail computed only from ACTIVE members; empty when the group no longer exists
     *  or has fewer than two active members (stale rows are removed). */
    @Transactional
    public Optional<GroupDetail> detailIfActive(Long groupId) {
        DupGroup group = dupGroupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return Optional.empty();
        }
        List<ImageRecord> activeMembers =
                imageRecordRepository.findByGroupIdAndStatus(groupId, ImageStatus.ACTIVE);
        if (activeMembers.size() < 2) {
            removeGroupSilently(groupId);
            return Optional.empty();
        }
        ImageRecord recommended = ImageRecommendation.recommend(activeMembers);
        List<Image> members = activeMembers.stream().map(Image::from).toList();
        GroupDetail detail = new GroupDetail(group.getId(), group.getScan().getId(),
                group.getCategory().name(), recommended.getId(), activeMembers.size(),
                ImageRecommendation.reclaimableBytes(activeMembers, recommended), members);
        return Optional.of(detail);
    }

    /** Clears every remaining member reference and deletes the (stale) group row. */
    private void removeGroupSilently(Long groupId) {
        if (!dupGroupRepository.existsById(groupId)) {
            return;
        }
        entityManager.createQuery(
                        "update ImageRecord r set r.group = null where r.group.id = :groupId")
                .setParameter("groupId", groupId)
                .executeUpdate();
        dupGroupRepository.deleteById(groupId);
    }
}