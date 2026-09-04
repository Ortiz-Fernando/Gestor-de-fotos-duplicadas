package com.imagedupmanager.web;

import com.imagedupmanager.service.RenamePreview;
import com.imagedupmanager.service.RenameService;
import com.imagedupmanager.web.dto.ApiDtos.RenameRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RenameController {

    private final RenameService renameService;

    public RenameController(RenameService renameService) {
        this.renameService = renameService;
    }

    @PostMapping("/api/images/{id}/rename/preview")
    public ResponseEntity<RenamePreview> preview(@PathVariable Long id,
                                                 @Valid @RequestBody RenameRequest request) {
        return ResponseEntity.ok(renameService.previewRename(id, request.newName()));
    }

    @PostMapping("/api/images/{id}/rename")
    public ResponseEntity<Map<String, String>> rename(@PathVariable Long id,
                                                      @Valid @RequestBody RenameRequest request) {
        renameService.rename(id, request.newName());
        return ResponseEntity.ok(Map.of("message", "Archivo renombrado correctamente."));
    }
}
