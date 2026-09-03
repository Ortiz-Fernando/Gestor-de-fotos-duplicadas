package com.imagedupmanager.service;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.Scan;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.repository.ScanRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persistence helper for scans. Kept as a separate Spring bean so that {@code @Transactional}
 * boundaries work from {@link ScanService} (self-invocation would bypass the proxy).
 */
@Component
public class ScanPersister {

    private final ScanRepository scanRepository;
    private final ImageRecordRepository imageRecordRepository;

    public ScanPersister(ScanRepository scanRepository, ImageRecordRepository imageRecordRepository) {
        this.scanRepository = scanRepository;
        this.imageRecordRepository = imageRecordRepository;
    }

    @Transactional
    public Scan create(Scan scan) {
        return scanRepository.save(scan);
    }

    /** Saves a batch of image records inside a single transaction, attaching the scan proxy. */
    @Transactional
    public void saveImageBatch(Long scanId, List<ImageRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Scan scanReference = scanRepository.getReferenceById(scanId);
        for (ImageRecord record : records) {
            record.setScan(scanReference);
        }
        imageRecordRepository.saveAll(records);
    }

    @Transactional
    public Scan save(Scan scan) {
        return scanRepository.save(scan);
    }
}
