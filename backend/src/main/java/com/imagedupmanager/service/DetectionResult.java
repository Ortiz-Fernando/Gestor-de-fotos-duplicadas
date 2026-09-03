package com.imagedupmanager.service;

/**
 * Summary returned by {@link DuplicateService} after analysing a scan.
 */
public final class DetectionResult {

    private final long scanId;
    private final int exactGroups;
    private final int exactImages;
    private final int visualGroups;
    private final int visualImages;
    private final int similarReviewPairs;
    private final int errors;
    private final long reclaimableBytes;

    public DetectionResult(long scanId, int exactGroups, int exactImages, int visualGroups,
                           int visualImages, int similarReviewPairs, int errors,
                           long reclaimableBytes) {
        this.scanId = scanId;
        this.exactGroups = exactGroups;
        this.exactImages = exactImages;
        this.visualGroups = visualGroups;
        this.visualImages = visualImages;
        this.similarReviewPairs = similarReviewPairs;
        this.errors = errors;
        this.reclaimableBytes = reclaimableBytes;
    }

    public long getScanId() {
        return scanId;
    }

    public int getExactGroups() {
        return exactGroups;
    }

    public int getExactImages() {
        return exactImages;
    }

    public int getVisualGroups() {
        return visualGroups;
    }

    public int getVisualImages() {
        return visualImages;
    }

    public int getSimilarReviewPairs() {
        return similarReviewPairs;
    }

    public int getErrors() {
        return errors;
    }

    public long getReclaimableBytes() {
        return reclaimableBytes;
    }
}
