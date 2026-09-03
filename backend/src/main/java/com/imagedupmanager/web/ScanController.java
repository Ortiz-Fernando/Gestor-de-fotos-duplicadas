package com.imagedupmanager.web;

import com.imagedupmanager.domain.Scan;
import com.imagedupmanager.domain.ScanStatus;
import com.imagedupmanager.repository.ScanRepository;
import com.imagedupmanager.service.DetectionResult;
import com.imagedupmanager.service.DuplicateService;
import com.imagedupmanager.service.ScanException;
import com.imagedupmanager.service.ScanService;
import com.imagedupmanager.web.dto.ApiDtos.CreateScanResponse;
import com.imagedupmanager.web.dto.ApiDtos.ScanSummary;
import com.imagedupmanager.web.dto.ApiDtos.StartScanRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/scans")
public class ScanController {

    private final ScanService scanService;
    private final DuplicateService duplicateService;
    private final ScanRepository scanRepository;

    public ScanController(ScanService scanService, DuplicateService duplicateService,
                          ScanRepository scanRepository) {
        this.scanService = scanService;
        this.duplicateService = duplicateService;
        this.scanRepository = scanRepository;
    }

    @PostMapping
    public ResponseEntity<CreateScanResponse> start(@Valid @RequestBody StartScanRequest request) {
        Path rootPath;
        try {
            rootPath = Path.of(request.rootPath());
        } catch (InvalidPathException e) {
            throw new ScanException("La ruta indicada no es válida.");
        }
        Long scanId = scanService.scanAsync(rootPath);
        return ResponseEntity.accepted()
                .location(URI.create("/api/scans/" + scanId))
                .body(new CreateScanResponse(scanId, ScanStatus.RUNNING));
    }

    @GetMapping
    public List<ScanSummary> list() {
        return scanRepository.findAllByOrderByStartedAtDesc().stream()
                .map(ScanSummary::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScanSummary> get(@PathVariable Long id) {
        Scan scan = scanRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No se ha encontrado el análisis solicitado."));
        return ResponseEntity.ok(ScanSummary.from(scan));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ScanSummary> cancel(@PathVariable Long id) {
        Scan scan = scanRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No se ha encontrado el análisis solicitado."));
        scanService.cancel(id);
        return ResponseEntity.ok(ScanSummary.from(scan));
    }

    @PostMapping("/{id}/detect")
    public ResponseEntity<DetectionResult> detect(@PathVariable Long id) {
        if (!scanRepository.existsById(id)) {
            throw new NotFoundException("No se ha encontrado el análisis solicitado.");
        }
        DetectionResult result = duplicateService.detect(id);
        return ResponseEntity.ok(result);
    }
}
