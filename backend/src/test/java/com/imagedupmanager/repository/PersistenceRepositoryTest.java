package com.imagedupmanager.repository;

import com.imagedupmanager.domain.AppSetting;
import com.imagedupmanager.domain.DupGroup;
import com.imagedupmanager.domain.DupGroupCategory;
import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.ImageStatus;
import com.imagedupmanager.domain.OperationLog;
import com.imagedupmanager.domain.OperationType;
import com.imagedupmanager.domain.Scan;
import com.imagedupmanager.domain.ScanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the persistence layer (JPA + Hibernate + SQLite).
 *
 * <p>Each test cleans all tables in {@code @BeforeEach}: SQLite/Hibernate test isolation
 * is not guaranteed through transaction rollback with the shared pooled context, so
 * every test starts from an empty database.
 */
@SpringBootTest
class PersistenceRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 3, 12, 0, 0);

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private ImageRecordRepository imageRecordRepository;

    @Autowired
    private DupGroupRepository dupGroupRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private AppSettingRepository appSettingRepository;

    @BeforeEach
    void cleanDatabase() {
        // Children first, then parents (foreign_keys is disabled on SQLite, but keep order safe).
        imageRecordRepository.deleteAllInBatch();
        dupGroupRepository.deleteAllInBatch();
        operationLogRepository.deleteAllInBatch();
        scanRepository.deleteAllInBatch();
        appSettingRepository.deleteAllInBatch();
    }

    private Scan savedScan() {
        return scanRepository.save(new Scan("C:\\Fotos", NOW.minusMinutes(10)));
    }

    private ImageRecord image(Scan scan, String name, String folder, long sizeBytes, String sha256) {
        ImageRecord record = new ImageRecord(scan, folder + "\\" + name, name, folder, "jpg",
                sizeBytes, NOW);
        record.setSha256(sha256);
        record.setWidth(1920);
        record.setHeight(1080);
        return record;
    }

    @Test
    void scanRoundTrip() {
        Scan scan = savedScan();
        assertNotNull(scan.getId());

        Scan loaded = scanRepository.findById(scan.getId()).orElseThrow();
        assertEquals("C:\\Fotos", loaded.getRootPath());
        assertEquals(ScanStatus.RUNNING, loaded.getStatus());

        loaded.setStatus(ScanStatus.COMPLETED);
        loaded.setFinishedAt(NOW);
        scanRepository.save(loaded);

        Scan reloaded = scanRepository.findById(scan.getId()).orElseThrow();
        assertEquals(ScanStatus.COMPLETED, reloaded.getStatus());
        assertEquals(NOW, reloaded.getFinishedAt());
    }

    @Test
    void imagesAreGroupedByScanAndQueryableBySha256() {
        Scan scan = savedScan();

        imageRecordRepository.save(image(scan, "a.jpg", "C:\\Fotos\\vacaciones", 1000, "aa"));
        imageRecordRepository.save(image(scan, "b.jpg", "C:\\Fotos\\copia", 1000, "aa"));
        imageRecordRepository.save(image(scan, "c.jpg", "C:\\Fotos\\varios", 2000, "bb"));

        assertEquals(3, imageRecordRepository.countByScanId(scan.getId()));
        List<ImageRecord> exact = imageRecordRepository.findByScanIdAndSha256(scan.getId(), "aa");
        assertEquals(2, exact.size());
        assertEquals(3, imageRecordRepository.countByScanIdAndSha256IsNotNull(scan.getId()));

        assertTrue(imageRecordRepository.existsByScanIdAndAbsolutePath(
                scan.getId(), "C:\\Fotos\\vacaciones\\a.jpg"));
        assertFalse(imageRecordRepository.existsByScanIdAndAbsolutePath(
                scan.getId(), "C:\\Fotos\\no-existe.jpg"));
    }

    @Test
    void uniqueAbsolutePathPerScanIsEnforced() {
        Scan scan = savedScan();
        imageRecordRepository.save(image(scan, "a.jpg", "C:\\Fotos", 1000, "aa"));
        try {
            // Same absolute path within the same scan -> must violate the unique index.
            imageRecordRepository.saveAndFlush(image(scan, "a.jpg", "C:\\Fotos", 2000, "bb"));
            org.junit.jupiter.api.Assertions.fail("Expected unique constraint violation");
        } catch (org.springframework.dao.DataAccessException expected) {
            // unique index on (scan_id, absolute_path) works
        }
    }

    @Test
    void dupGroupLinksImagesAndKeepsRecommendation() {
        Scan scan = savedScan();
        ImageRecord first = imageRecordRepository.save(image(scan, "a.jpg", "C:\\Fotos", 1000, "aa"));
        ImageRecord second = imageRecordRepository.save(image(scan, "b.jpg", "C:\\Fotos", 1000, "aa"));

        DupGroup group = dupGroupRepository.save(new DupGroup(scan, DupGroupCategory.EXACT));
        group.setMemberCount(2);
        group.setRecommendedImageId(first.getId());
        group.setReclaimableBytes(second.getSizeBytes());
        dupGroupRepository.save(group);

        first.setGroup(group);
        second.setGroup(group);
        imageRecordRepository.save(first);
        imageRecordRepository.save(second);

        List<DupGroup> groups = dupGroupRepository.findByScanId(scan.getId());
        assertEquals(1, groups.size());
        assertEquals(DupGroupCategory.EXACT, groups.get(0).getCategory());
        assertEquals(2, groups.get(0).getMemberCount());
        assertEquals(first.getId(), groups.get(0).getRecommendedImageId());

        List<ImageRecord> members = imageRecordRepository.findByGroupId(group.getId());
        assertEquals(2, members.size());
    }

    @Test
    void operationLogRoundTrip() {
        Scan scan = savedScan();
        ImageRecord imageRecord = imageRecordRepository.save(image(scan, "a.jpg", "C:\\Fotos", 1000, "aa"));

        OperationLog log = operationLogRepository.save(new OperationLog(
                OperationType.RENAME,
                imageRecord.getId(),
                "C:\\Fotos\\a.jpg",
                "C:\\Fotos\\a-renombrada.jpg",
                NOW));
        log.setReversible(true);

        List<OperationLog> all = operationLogRepository.findAllByOrderByOperationTimeDesc();
        assertFalse(all.isEmpty());
        assertEquals(OperationType.RENAME, all.get(0).getType());
        assertEquals(1, operationLogRepository.findByImageId(imageRecord.getId()).size());
    }

    @Test
    void appSettingRoundTrip() {
        appSettingRepository.save(new AppSetting("ultimaCarpeta", "C:\\Fotos"));

        AppSetting setting = appSettingRepository.findById("ultimaCarpeta").orElseThrow();
        assertEquals("C:\\Fotos", setting.getValue());

        setting.setValue("D:\\Backup");
        appSettingRepository.save(setting);
        assertEquals("D:\\Backup", appSettingRepository.findById("ultimaCarpeta").orElseThrow().getValue());
    }

    @Test
    void imageStatusEnumRoundTrip() {
        Scan scan = savedScan();
        ImageRecord record = image(scan, "papelera.jpg", "C:\\Fotos", 500, null);
        record.setStatus(ImageStatus.IN_TRASH);
        imageRecordRepository.save(record);

        ImageRecord loaded = imageRecordRepository.findById(record.getId()).orElseThrow();
        assertEquals(ImageStatus.IN_TRASH, loaded.getStatus());
    }
}
