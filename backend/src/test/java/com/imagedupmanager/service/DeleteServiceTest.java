package com.imagedupmanager.service;

import com.imagedupmanager.domain.DupGroup;
import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.ImageStatus;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for sending images to the trash (Fase 12). The Windows shell integration is
 * mocked so tests never touch the real Recycle Bin.
 */
@SpringBootTest
class DeleteServiceTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ScanService scanService;

    @Autowired
    private DeleteService deleteService;

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private ImageRecordRepository imageRecordRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private DupGroupRepository dupGroupRepository;

    @Autowired
    private DuplicateService duplicateService;

    @MockitoBean
    private FileTrash fileTrash;

    @BeforeEach
    void cleanDatabase() {
        imageRecordRepository.deleteAllInBatch();
        dupGroupRepository.deleteAllInBatch();
        operationLogRepository.deleteAllInBatch();
        scanRepository.deleteAllInBatch();
    }

    private ImageRecord scanSingleImage(String fileName) throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("raiz-papelera"));
        Path file = root.resolve(fileName);
        Files.write(file, "foto".getBytes(StandardCharsets.UTF_8));
        Scan scan = scanService.scanSync(root);
        return imageRecordRepository.findByScanId(scan.getId()).get(0);
    }

    @Test
    void sendToTrashMarksRecordAndLogsOperation() throws IOException {
        ImageRecord record = scanSingleImage("foto.jpg");
        Path file = Path.of(record.getAbsolutePath());

        deleteService.sendToTrash(record.getId());

        verify(fileTrash).sendToTrash(any(Path.class));
        assertTrue(Files.exists(file), "el archivo físico no se borra (lo gestiona la Papelera)");

        ImageRecord updated = imageRecordRepository.findById(record.getId()).orElseThrow();
        assertEquals(ImageStatus.IN_TRASH, updated.getStatus());

        List<OperationLog> logs = operationLogRepository.findByImageId(record.getId());
        assertEquals(1, logs.size());
        assertEquals(OperationType.TRASH, logs.get(0).getType());
        assertFalse(logs.get(0).isReversible());
    }

    @Test
    void missingFileIsRejectedWithoutCallingShell() throws IOException {
        ImageRecord record = scanSingleImage("foto.jpg");
        Path file = Path.of(record.getAbsolutePath());
        Files.delete(file);

        assertThrows(OperationException.class,
                () -> deleteService.sendToTrash(record.getId()));
        verify(fileTrash, never()).sendToTrash(any(Path.class));
    }

    /** Scans {@code names} (identical content) and runs duplicate detection. */
    private List<ImageRecord> scanExactDuplicates(String dirName, String... names) throws IOException {
        Path root = Files.createDirectories(tempDir.resolve(dirName));
        byte[] content = "mismo-contenido".getBytes(StandardCharsets.UTF_8);
        for (String name : names) {
            Files.write(root.resolve(name), content);
        }
        Scan scan = scanService.scanSync(root);
        duplicateService.detect(scan.getId());
        return imageRecordRepository.findByScanId(scan.getId());
    }

    @Test
    void trashingOneOfTwoExactDuplicatesDeletesTheGroup() throws IOException {
        List<ImageRecord> records = scanExactDuplicates("duplicados-2", "a.jpg", "b.jpg");
        assertEquals(2, records.size());
        Long groupId = records.get(0).getGroup().getId();
        assertEquals(groupId, records.get(1).getGroup().getId());

        deleteService.sendToTrash(records.get(0).getId());

        ImageRecord trashed = imageRecordRepository.findById(records.get(0).getId()).orElseThrow();
        assertEquals(ImageStatus.IN_TRASH, trashed.getStatus());
        assertNull(trashed.getGroup(), "la imagen en la papelera debe desvincularse del grupo");

        ImageRecord kept = imageRecordRepository.findById(records.get(1).getId()).orElseThrow();
        assertEquals(ImageStatus.ACTIVE, kept.getStatus());
        assertNull(kept.getGroup());

        assertTrue(dupGroupRepository.findById(groupId).isEmpty(),
                "el grupo debe desaparecer al quedar con una sola imagen activa");
    }

    @Test
    void trashingOneOfThreeExactDuplicatesKeepsAndRefreshesTheGroup() throws IOException {
        List<ImageRecord> records = scanExactDuplicates("duplicados-3", "a.jpg", "b.jpg", "c.jpg");
        assertEquals(3, records.size());
        DupGroup group = dupGroupRepository.findByScanId(records.get(0).getScan().getId()).get(0);
        Long recommendedId = group.getRecommendedImageId();
        assertEquals(3, group.getMemberCount());

        deleteService.sendToTrash(recommendedId);

        DupGroup updated = dupGroupRepository.findById(group.getId()).orElseThrow();
        assertEquals(2, updated.getMemberCount());
        assertFalse(updated.getRecommendedImageId().equals(recommendedId),
                "si la recomendada se envía a la papelera, debe elegirse otra activa");

        List<ImageRecord> members = imageRecordRepository.findByGroupId(group.getId());
        assertEquals(2, members.size());
        assertTrue(members.stream().allMatch(member -> member.getStatus() == ImageStatus.ACTIVE));
    }

    @Test
    void detectIgnoresImagesAlreadySentToTrash() throws IOException {
        List<ImageRecord> records = scanExactDuplicates("duplicados-redetect", "a.jpg", "b.jpg");
        assertEquals(2, records.size());
        Long scanId = records.get(0).getScan().getId();

        deleteService.sendToTrash(records.get(0).getId());

        DetectionResult result = duplicateService.detect(scanId);
        assertEquals(0, result.getExactGroups(),
                "la detección no debe reintroducir imágenes ya enviadas a la papelera");
        assertTrue(dupGroupRepository.findByScanId(scanId).isEmpty());
    }
}
