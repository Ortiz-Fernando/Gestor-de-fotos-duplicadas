package com.imagedupmanager.service;

import com.imagedupmanager.domain.ImageRecord;

import java.util.List;

/**
 * Suggests which image of a duplicate group to keep (AGENTS.md #27). Shared by the
 * detection pass ({@link DuplicateService}) and the group refresh after an image is sent
 * to the trash ({@link GroupService}), so both always apply the same criteria:
 * highest resolution, then largest file, then deterministic path.
 */
public final class ImageRecommendation {

    private ImageRecommendation() {
    }

    /** Suggests the file to keep: highest resolution, then largest, then deterministic path. */
    public static ImageRecord recommend(List<ImageRecord> members) {
        ImageRecord best = members.get(0);
        for (int i = 1; i < members.size(); i++) {
            ImageRecord candidate = members.get(i);
            if (isBetter(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    /** Bytes that could be reclaimed by keeping only the recommended image. */
    public static long reclaimableBytes(List<ImageRecord> members, ImageRecord recommended) {
        long totalBytes = 0;
        for (ImageRecord member : members) {
            totalBytes += member.getSizeBytes();
        }
        return totalBytes - recommended.getSizeBytes();
    }

    private static boolean isBetter(ImageRecord candidate, ImageRecord current) {
        long candidateArea = visualArea(candidate);
        long currentArea = visualArea(current);
        if (candidateArea != currentArea) {
            return candidateArea > currentArea;
        }
        if (candidate.getSizeBytes() != current.getSizeBytes()) {
            return candidate.getSizeBytes() > current.getSizeBytes();
        }
        return candidate.getAbsolutePath().compareToIgnoreCase(current.getAbsolutePath()) < 0;
    }

    private static long visualArea(ImageRecord record) {
        if (record.getWidth() != null && record.getHeight() != null) {
            return (long) record.getWidth() * record.getHeight();
        }
        return -1L;
    }
}