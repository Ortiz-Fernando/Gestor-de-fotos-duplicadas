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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for operation history and undo (Fase 12).
 */
@SpringBootTest
class OperationServiceTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ScanService scanService;

    @Autowired
    private RenameService renameService;

    @Autowired
    private OperationService operationService;

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
        Path root = Files.createDirectories(tempDir.resolve("raiz-operaciones"));
        Files.write(root.resolve(fileName),
                "foto".getBytes(StandardCharsets.UTF_8));
        Scan scan = scanService.scanSync(root);
        return imageRecordRepository.findByScanId(scan.getId()).get(0);
    }

    @Test
    void undoRestoresRenamedFileAndUpdatesRecord() throws IOException {
        ImageRecord record = scanSingleImage("original.jpg");
        renameService.rename(record.getId(), "nuevo");
        Path originalPath = Path.of(record.getAbsolutePath());
        Path renamedPath = originalPath.resolveSibling("nuevo.jpg");
        assertTrue(Files.exists(renamedPath));

        OperationLog renameLog = operationLogRepository.findByImageId(record.getId()).get(0);
        assertEquals(OperationType.RENAME, renameLog.getType());

        operationService.undo(renameLog.getId());

        assertTrue(Files.exists(originalPath), "el archivo debe volver a su nombre original");
        assertFalse(Files.exists(renamedPath));

        ImageRecord updated = imageRecordRepository.findById(record.getId()).orElseThrow();
        assertEquals("original.jpg", updated.getName());

        OperationLog reloaded = operationLogRepository.findById(renameLog.getId()).orElseThrow();
        assertNotNull(reloaded.getUndoneAt(), "la operación debe marcarse como deshecha");

        List<OperationLog> history = operationService.history();
        assertTrue(history.stream().anyMatch(log -> log.getType() == OperationType.UNDO));
    }

    @Test
    void trashOperationCannotBeUndoneAutomatically() throws IOException {
        ImageRecord record = scanSingleImage("foto.jpg");
        OperationLog trashLog = new OperationLog(OperationType.TRASH, record.getId(),
                record.getAbsolutePath(), null, LocalDateTime.now());
        trashLog.setReversible(false);
        operationLogRepository.save(trashLog);

        assertThrows(OperationException.class, () -> operationService.undo(trashLog.getId()));
        assertTrue(Files.exists(Path.of(record.getAbsolutePath())));
    }

    @Test
    void undoingTwiceIsRejected() throws IOException {
        ImageRecord record = scanSingleImage("original.jpg");
        renameService.rename(record.getId(), "nuevo");
        OperationLog renameLog = operationLogRepository.findByImageId(record.getId()).get(0);

        operationService.undo(renameLog.getId());
        assertThrows(OperationException.class, () -> operationService.undo(renameLog.getId()));
    }
}
