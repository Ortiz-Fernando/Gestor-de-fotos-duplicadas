package com.imagedupmanager.web;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.web.dto.ApiDtos.Image;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageRecordRepository imageRecordRepository;

    public ImageController(ImageRecordRepository imageRecordRepository) {
        this.imageRecordRepository = imageRecordRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Image> get(@PathVariable Long id) {
        ImageRecord record = imageRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No se ha encontrado la imagen solicitada."));
        return ResponseEntity.ok(Image.from(record));
    }
}
