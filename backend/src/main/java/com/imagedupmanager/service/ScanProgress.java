package com.imagedupmanager.service;

import com.imagedupmanager.domain.ScanStatus;

/**
 * Immutable snapshot of the progress of a scan, suitable for reporting to clients.
 */
public final class ScanProgress {

    private final Long scanId;
    private final ScanStatus status;
    private final int discoveredFiles;
    private final int storedFiles;
    private final int errorCount;
    private final String currentPath;

    public ScanProgress(Long scanId, ScanStatus status, int discoveredFiles, int storedFiles,
                        int errorCount, String currentPath) {
        this.scanId = scanId;
        this.status = status;
        this.discoveredFiles = discoveredFiles;
        this.storedFiles = storedFiles;
        this.errorCount = errorCount;
        this.currentPath = currentPath;
    }

    public Long getScanId() {
        return scanId;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public int getDiscoveredFiles() {
        return discoveredFiles;
    }

    public int getStoredFiles() {
        return storedFiles;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public String getCurrentPath() {
        return currentPath;
    }
}
