package com.imagedupmanager.service;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.Scan;
import com.imagedupmanager.domain.ScanStatus;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.repository.ScanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the recursive file system scanner (Fase 4).
 */
@SpringBootTest
class ScanServiceTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ScanService scanService;

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private ImageRecordRepository imageRecordRepository;

    @BeforeEach
    void cleanDatabase() {
        imageRecordRepository.deleteAllInBatch();
        scanRepository.deleteAllInBatch();
    }

    private void write(Path file, byte[] content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content);
    }

    @Test
    void scanStoresOnlyImagesRecursively() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("album"));
        write(root.resolve("vacaciones 2026/playa.JPG"), bytes());
        write(root.resolve("carpeta-ñ/boda 02/foto.png"), bytes());
        write(root.resolve("carpeta-ñ/boda 02/foto.gif"), bytes());
        write(root.resolve("portada.webp"), bytes());
        write(root.resolve("DSC_0001.CR2"), bytes());
        write(root.resolve("IMG_0002.heic"), bytes());
        write(root.resolve("notas.txt"), bytes());
        write(root.resolve("datos.csv"), bytes());

        Scan scan = scanService.scanSync(root);

        List<ImageRecord> stored = imageRecordRepository.findByScanId(scan.getId());
        String names = stored.stream().map(ImageRecord::getName).sorted().toList().toString();

        assertEquals(ScanStatus.COMPLETED, scan.getStatus(), "scan status");
        assertNull(scan.getErrorMessage(), "scan error");
        assertEquals(6, stored.size(), "stored records: " + names);
        assertEquals(6, scan.getFileCount(), "file count: " + names);
        assertEquals(0, scan.getErrorCount(), "error count");
        assertTrue(stored.stream().anyMatch(r -> r.getAbsolutePath().contains("carpeta-ñ")));
        assertTrue(stored.stream().anyMatch(r -> r.getName().equals("playa.JPG")));
        assertFalse(stored.stream().anyMatch(r -> r.getName().endsWith(".txt")));
    }

    @Test
    void analysableFlagsFollowSupportedFormats() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("formatos"));
        write(root.resolve("a.jpg"), bytes());
        write(root.resolve("b.PNG"), bytes());
        write(root.resolve("c.webp"), bytes());
        write(root.resolve("d.tiff"), bytes());
        write(root.resolve("e.heic"), bytes());
        write(root.resolve("f.cr2"), bytes());

        Scan scan = scanService.scanSync(root);
        assertEquals(6, scan.getFileCount());

        List<ImageRecord> stored = imageRecordRepository.findByScanId(scan.getId());
        assertEquals(6, stored.size());
        for (ImageRecord record : stored) {
            boolean visuallySupported =
                    SupportedImageFormats.isVisuallyAnalysable(record.getExtension());
            assertEquals(visuallySupported, record.isAnalysable(),
                    "analysable mismatch for " + record.getName());
        }
    }

    @Test
    void emptyDirectoryCompletesWithoutErrors() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("vacia"));
        Scan scan = scanService.scanSync(root);

        assertEquals(ScanStatus.COMPLETED, scan.getStatus());
        assertEquals(0, scan.getFileCount());
        assertEquals(0L, imageRecordRepository.countByScanId(scan.getId()));
    }

    @Test
    void missingRootIsRejectedWithUserMessage() {
        Path missing = tempDir.resolve("no-existe");
        ScanException exception = assertThrows(ScanException.class,
                () -> scanService.scanSync(missing));
        assertTrue(exception.getMessage().toLowerCase().contains("carpeta"));
    }

    @Test
    void fileAsRootIsRejectedWithUserMessage() throws IOException {
        Path file = Files.write(tempDir.resolve("fichero.jpg"), bytes());
        ScanException exception = assertThrows(ScanException.class,
                () -> scanService.scanSync(file));
        assertTrue(exception.getMessage().toLowerCase().contains("carpeta"));
    }

    @Test
    void asyncScanCanBeCancelled() throws IOException, InterruptedException {
        Path root = Files.createDirectories(tempDir.resolve("muchas"));
        for (int folder = 0; folder < 10; folder++) {
            Path dir = root.resolve("sub-" + folder);
            Files.createDirectories(dir);
            for (int file = 0; file < 100; file++) {
                Files.write(dir.resolve(String.format("img-%03d.jpg", file)), bytes());
            }
        }

        Long scanId = scanService.scanAsync(root);
        scanService.cancel(scanId);

        ScanStatus terminal = awaitTerminal(scanId, Duration.ofSeconds(30));
        assertEquals(ScanStatus.CANCELLED, terminal);

        Scan scan = scanRepository.findById(scanId).orElseThrow();
        assertEquals(ScanStatus.CANCELLED, scan.getStatus());
        assertTrue(scan.getErrorMessage() != null
                && scan.getErrorMessage().toLowerCase().contains("cancelado"));
    }

    private ScanStatus awaitTerminal(Long scanId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            ScanStatus status = scanService.getProgress(scanId)
                    .map(ScanProgress::getStatus)
                    .orElse(null);
            if (status == ScanStatus.COMPLETED
                    || status == ScanStatus.FAILED
                    || status == ScanStatus.CANCELLED) {
                return status;
            }
            Thread.sleep(50);
        }
        return null;
    }

    private byte[] bytes() {
        return "contenido-de-prueba".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
