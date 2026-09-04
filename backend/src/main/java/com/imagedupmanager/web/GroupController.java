package com.imagedupmanager.web;

import com.imagedupmanager.service.GroupService;
import com.imagedupmanager.web.dto.ApiDtos.GroupDetail;
import com.imagedupmanager.web.dto.ApiDtos.GroupSummary;
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

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/scans/{scanId}/groups")
    public List<GroupSummary> groupsByScan(@PathVariable Long scanId,
                                           @RequestParam(required = false) String category) {
        return groupService.summariesForScan(scanId, category);
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<GroupDetail> detail(@PathVariable Long id) {
        GroupDetail detail = groupService.detailIfActive(id)
                .orElseThrow(() -> new NotFoundException("No se ha encontrado el grupo solicitado."));
        return ResponseEntity.ok(detail);
    }
}
