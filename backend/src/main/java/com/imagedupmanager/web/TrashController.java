package com.imagedupmanager.web;

import com.imagedupmanager.service.DeleteService;
import com.imagedupmanager.web.dto.ApiDtos.TrashRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TrashController {

    private final DeleteService deleteService;

    public TrashController(DeleteService deleteService) {
        this.deleteService = deleteService;
    }

    /** Sends an image to the OS Recycle Bin. Requires explicit confirmation. */
    @PostMapping("/api/images/{id}/trash")
    public ResponseEntity<Map<String, String>> trash(@PathVariable Long id,
                                                     @Valid @RequestBody TrashRequest request) {
        deleteService.sendToTrash(id);
        return ResponseEntity.ok(Map.of("message", "Imagen enviada a la Papelera."));
    }
}
