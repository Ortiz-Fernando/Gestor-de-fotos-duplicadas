package com.imagedupmanager.service;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.OperationLog;
import com.imagedupmanager.domain.OperationType;
import com.imagedupmanager.domain.Scan;
import com.imagedupmanager.repository.DupGroupRepository;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.repository.OperationLogRepository;
import com.imagedupmanager.repository.ScanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the safe rename service (Fase 11).
 */
@SpringBootTest
class RenameServiceTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ScanService scanService;

    @Autowired
    private RenameService renameService;

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private ImageRecordRepository imageRecordRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private DupGroupRepository dupGroupRepository;

    @BeforeEach
    void cleanDatabase() {
        imageRecordRepository.deleteAllInBatch();
        dupGroupRepository.deleteAllInBatch();
        operationLogRepository.deleteAllInBatch();
        scanRepository.deleteAllInBatch();
    }

    private ImageRecord scanSingleImage(String fileName) throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("raiz-" + fileName));
        Files.write(root.resolve(fileName),
                "contenido-de-imagen".getBytes(StandardCharsets.UTF_8));
        Scan scan = scanService.scanSync(root);
        return imageRecordRepository.findByScanId(scan.getId()).get(0);
    }

    @Test
    void renameAppliesKeepsExtensionAndLogsOperation() throws IOException {
        ImageRecord record = scanSingleImage("original.jpg");
        Path source = Path.of(record.getAbsolutePath());

        RenamePreview preview = renameService.previewRename(record.getId(), "vacaciones 2026");
        assertEquals("vacaciones 2026.jpg", preview.getNewName());
        assertFalse(Files.exists(Path.of(preview.getNewPath())));

        renameService.rename(record.getId(), "vacaciones 2026");

        Path target = source.resolveSibling("vacaciones 2026.jpg");
        assertTrue(Files.isRegularFile(target), "el archivo debe existir renombrado");
        assertFalse(Files.exists(source), "el archivo original no debe existir");

        ImageRecord updated = imageRecordRepository.findById(record.getId()).orElseThrow();
        assertEquals("vacaciones 2026.jpg", updated.getName());
        assertTrue(updated.getAbsolutePath().endsWith("vacaciones 2026.jpg"));

        List<OperationLog> logs = operationLogRepository.findByImageId(record.getId());
        assertEquals(1, logs.size());
        assertEquals(OperationType.RENAME, logs.get(0).getType());
        assertTrue(logs.get(0).isReversible());
    }

    @Test
    void renameDoesNotOverwriteExistingFile() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("raiz-conflicto"));
        Files.write(root.resolve("a.jpg"), "foto-a".getBytes(StandardCharsets.UTF_8));
        Files.write(root.resolve("b.jpg"), "foto-b".getBytes(StandardCharsets.UTF_8));
        Scan scan = scanService.scanSync(root);

        List<ImageRecord> records = imageRecordRepository.findByScanId(scan.getId());
        ImageRecord imageA = records.stream()
                .filter(image -> image.getName().equals("a.jpg")).findFirst().orElseThrow();

        assertThrows(OperationException.class,
                () -> renameService.previewRename(imageA.getId(), "b"));

        assertTrue(Files.exists(root.resolve("a.jpg")), "a.jpg no debe desaparecer");
        assertTrue(Files.exists(root.resolve("b.jpg")), "b.jpg no debe sobrescribirse");
        assertEquals(0, operationLogRepository.findByImageId(imageA.getId()).size());
    }

    @Test
    void invalidNamesAreRejected() throws IOException {
        ImageRecord record = scanSingleImage("foto.jpg");
        Path source = Path.of(record.getAbsolutePath());

        assertThrows(OperationException.class,
                () -> renameService.previewRename(record.getId(), "mal*nombre"));
        assertThrows(OperationException.class,
                () -> renameService.previewRename(record.getId(), "foto."));
        assertThrows(OperationException.class,
                () -> renameService.previewRename(record.getId(), "CON"));
        assertThrows(OperationException.class,
                () -> renameService.previewRename(record.getId(), "otra.png"));
        assertTrue(Files.exists(source));
    }

    @Test
    void missingSourceFileIsRejected() throws IOException {
        ImageRecord record = scanSingleImage("foto.jpg");
        Path source = Path.of(record.getAbsolutePath());
        Files.delete(source);

        assertThrows(OperationException.class,
                () -> renameService.previewRename(record.getId(), "nuevo.jpg"));
    }
}
