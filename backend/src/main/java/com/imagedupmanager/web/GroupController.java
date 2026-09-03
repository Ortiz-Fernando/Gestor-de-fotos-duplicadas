package com.imagedupmanager.web;

import com.imagedupmanager.domain.DupGroup;
import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.repository.DupGroupRepository;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.web.dto.ApiDtos.GroupDetail;
import com.imagedupmanager.web.dto.ApiDtos.GroupSummary;
import com.imagedupmanager.web.dto.ApiDtos.Image;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GroupController {

    private final DupGroupRepository dupGroupRepository;
    private final ImageRecordRepository imageRecordRepository;

    public GroupController(DupGroupRepository dupGroupRepository,
                           ImageRecordRepository imageRecordRepository) {
        this.dupGroupRepository = dupGroupRepository;
        this.imageRecordRepository = imageRecordRepository;
    }

    @GetMapping("/scans/{scanId}/groups")
    public List<GroupSummary> groupsByScan(@PathVariable Long scanId,
                                           @RequestParam(required = false) String category) {
        List<DupGroup> groups = (category == null || category.isBlank())
                ? dupGroupRepository.findByScanId(scanId)
                : dupGroupRepository.findByScanIdAndCategory(scanId,
                        com.imagedupmanager.domain.DupGroupCategory.valueOf(category));
        return groups.stream().map(this::toSummary).toList();
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<GroupDetail> detail(@PathVariable Long id) {
        DupGroup group = dupGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No se ha encontrado el grupo solicitado."));
        List<Image> members = imageRecordRepository.findByGroupId(group.getId()).stream()
                .map(Image::from)
                .toList();
        GroupDetail detail = new GroupDetail(group.getId(), group.getScan().getId(),
                group.getCategory().name(), group.getRecommendedImageId(),
                group.getMemberCount(), group.getReclaimableBytes(), members);
        return ResponseEntity.ok(detail);
    }

    private GroupSummary toSummary(DupGroup group) {
        return new GroupSummary(group.getId(), group.getScan().getId(),
                group.getCategory().name(), group.getRecommendedImageId(),
                group.getMemberCount(), group.getReclaimableBytes());
    }
}
