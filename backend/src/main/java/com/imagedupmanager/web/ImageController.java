package com.imagedupmanager.web;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.web.dto.ApiDtos.Image;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    /** Streams the original file content for display (local only, no copies). */
    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable Long id) throws IOException {
        ImageRecord record = imageRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No se ha encontrado la imagen solicitada."));
        Path path = Paths.get(record.getAbsolutePath());
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException("El archivo de la imagen ya no está disponible.");
        }
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String probed = Files.probeContentType(path);
        if (probed != null) {
            mediaType = MediaType.parseMediaType(probed);
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new FileSystemResource(path));
    }
}

