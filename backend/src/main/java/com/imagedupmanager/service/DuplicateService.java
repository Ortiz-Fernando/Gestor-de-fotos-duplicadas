package com.imagedupmanager.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.imagedupmanager.config.DuplicateProperties;
import com.imagedupmanager.domain.DupGroup;
import com.imagedupmanager.domain.DupGroupCategory;
import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.Scan;
import com.imagedupmanager.hashing.ExifOrientationNormalizer;
import com.imagedupmanager.hashing.HammingDistance;
import com.imagedupmanager.hashing.HashingException;
import com.imagedupmanager.hashing.ImagePerceptualHasher;
import com.imagedupmanager.hashing.Sha256CacheValidator;
import com.imagedupmanager.hashing.Sha256Hasher;
import com.imagedupmanager.repository.DupGroupRepository;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.repository.ScanRepository;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

/**
 * Duplicate detection and grouping for a completed scan (AGENTS.md #13, ADR D3/D7).
 *
 * <p>Exact duplicates are detected through SHA-256 over same-size candidates. Visual
 * duplicates use the 64-bit perceptual hash and the configured thresholds. Nothing is ever
 * deleted or flagged as disposable based on pHash alone.
 */
@Service
public class DuplicateService {

    private final ScanRepository scanRepository;
    private final ImageRecordRepository imageRecordRepository;
    private final DupGroupRepository dupGroupRepository;
    private final DuplicateUpdater updater;
    private final ImagePerceptualHasher hasher;
    private final DuplicateProperties properties;

    public DuplicateService(ScanRepository scanRepository,
                            ImageRecordRepository imageRecordRepository,
                            DupGroupRepository dupGroupRepository,
                            DuplicateUpdater updater,
                            ImagePerceptualHasher hasher,
                            DuplicateProperties properties) {
        this.scanRepository = scanRepository;
        this.imageRecordRepository = imageRecordRepository;
        this.dupGroupRepository = dupGroupRepository;
        this.updater = updater;
        this.hasher = hasher;
        this.properties = properties;
    }

    /**
     * Runs the full detection for the given scan: exact (SHA-256) and visual (pHash)
     * grouping. Idempotent: previous groups of the scan are cleared first.
     */
    public DetectionResult detect(Long scanId) {
        scanRepository.findById(scanId)
                .orElseThrow(() -> new ScanException("El análisis seleccionado no existe."));
        resetForScan(scanId);

        List<ImageRecord> records = imageRecordRepository.findByScanIdOrderByAbsolutePathAsc(scanId);
        Counters counters = new Counters();

        // 1) SHA-256 pass (streaming, with safe cache reuse).
        hashAllRecords(records, counters);

        // 2) Exact duplicate groups (SHA-256).
        Map<String, List<ImageRecord>> bySha = new HashMap<>();
        for (ImageRecord record : records) {
            if (record.getSha256() != null) {
                bySha.computeIfAbsent(record.getSha256(), key -> new ArrayList<>()).add(record);
            }
        }
        Set<Long> exactMemberIds = new HashSet<>();
        for (List<ImageRecord> group : bySha.values()) {
            if (group.size() >= 2) {
                persistGroup(DupGroupCategory.EXACT, group, counters);
                group.forEach(member -> exactMemberIds.add(member.getId()));
            }
        }

        // 3) Perceptual pass + visual grouping for the remaining analysable images.
        List<ImageRecord> visualCandidates = ensurePerceptual(records, exactMemberIds, counters);
        groupVisual(visualCandidates, counters);

        return new DetectionResult(scanId, counters.exactGroups, counters.exactImages,
                counters.visualGroups, counters.visualImages, counters.similarReviewPairs,
                counters.errors, counters.reclaimableBytes);
    }

    /** Clears group assignments and removes previous groups of the scan. */
    private void resetForScan(Long scanId) {
        updater.clearGroupAssignments(scanId);
        updater.deleteGroupsForScan(scanId);
    }

    /** Ensures every record has a valid SHA-256, reusing safe cache entries (Fase 5). */
    private void hashAllRecords(List<ImageRecord> records, Counters counters) {
        for (ImageRecord record : records) {
            Path path = Paths.get(record.getAbsolutePath());
            if (record.getSha256() != null
                    && Sha256CacheValidator.isUsable(record.getSizeBytes(),
                    record.getLastModified(), path)) {
                continue;
            }
            try {
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                if (!attributes.isRegularFile()) {
                    counters.errors++;
                    continue;
                }
                String sha256 = Sha256Hasher.of(path);
                LocalDateTime lastModified =
                        LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneOffset.UTC);
                record.setSha256(sha256);
                record.setSizeBytes(attributes.size());
                record.setLastModified(lastModified);
                updater.updateShaAndAttributes(record.getId(), sha256, attributes.size(), lastModified);
            } catch (IOException | HashingException e) {
                counters.errors++;
            }
        }
    }

    /**
     * Ensures perceptual hashes and dimensions for analysable records not already in an
     * exact group. Decoding is done image by image and released immediately.
     */
    private List<ImageRecord> ensurePerceptual(List<ImageRecord> records, Set<Long> exactMemberIds,
                                               Counters counters) {
        List<ImageRecord> candidates = new ArrayList<>();
        for (ImageRecord record : records) {
            if (exactMemberIds.contains(record.getId()) || !record.isAnalysable()) {
                continue;
            }
            Path path = Paths.get(record.getAbsolutePath());
            boolean hashValid = record.getPhash() != null
                    && Sha256CacheValidator.isUsable(record.getSizeBytes(),
                    record.getLastModified(), path);
            boolean dimensionsKnown = record.getWidth() != null && record.getHeight() != null;
            if (hashValid && dimensionsKnown) {
                candidates.add(record);
                continue;
            }
            try {
                Decoded decoded = decodeOriented(path);
                long phash = hasher.hash(decoded.image());
                record.setPhash(phash);
                record.setWidth(decoded.image().getWidth());
                record.setHeight(decoded.image().getHeight());
                record.setExifOrientation(decoded.orientation());
                updater.updatePerceptual(record.getId(), phash, decoded.image().getWidth(),
                        decoded.image().getHeight(), decoded.orientation());
                candidates.add(record);
            } catch (IOException | HashingException e) {
                counters.errors++;
            }
        }
        return candidates;
    }

    /** Decodes an image and applies its EXIF orientation (AGENTS.md #19). */
    private Decoded decodeOriented(Path path) throws IOException {
        BufferedImage raw = ImageIO.read(path.toFile());
        if (raw == null) {
            throw new IOException("Formato de imagen no reconocido: " + path);
        }
        Integer orientation = readExifOrientation(path);
        BufferedImage oriented = ExifOrientationNormalizer.normalize(raw, orientation);
        return new Decoded(oriented, orientation);
    }

    private Integer readExifOrientation(Path path) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
            ExifIFD0Directory directory =
                    metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Controlled visual grouping: each image only joins an existing group when its Hamming
     * distance to the group REPRESENTATIVE is within the threshold (no blind transitive
     * chains). Pairs in the review band are counted for human review, not grouped.
     */
    private void groupVisual(List<ImageRecord> candidates, Counters counters) {
        if (candidates.isEmpty()) {
            return;
        }
        int threshold = properties.getPerceptual().getThreshold();
        int reviewThreshold = properties.getPerceptual().getReviewThreshold();

        List<VisualGroupData> groups = new ArrayList<>();
        for (ImageRecord candidate : candidates) {
            VisualGroupData matched = null;
            for (VisualGroupData group : groups) {
                ImageRecord representative = group.representative;
                if (!dimensionsCompatible(representative, candidate)) {
                    continue;
                }
                int distance =
                        HammingDistance.of(representative.getPhash(), candidate.getPhash());
                if (distance <= threshold) {
                    matched = group;
                    break;
                }
                if (distance <= reviewThreshold) {
                    counters.similarReviewPairs++;
                }
            }
            if (matched == null) {
                groups.add(new VisualGroupData(candidate));
            } else {
                matched.members.add(candidate);
            }
        }

        for (VisualGroupData group : groups) {
            if (group.members.size() >= 2) {
                persistGroup(DupGroupCategory.POSSIBLE_VISUAL, group.members, counters);
            }
        }
    }

    /** Conservative dimension pre-filter (ADR D7): prunes clearly incompatible pairs. */
    private boolean dimensionsCompatible(ImageRecord first, ImageRecord second) {
        if (!properties.getVisual().isDimensionFilterEnabled()) {
            return true;
        }
        if (first.getWidth() == null || first.getHeight() == null
                || second.getWidth() == null || second.getHeight() == null) {
            return true;
        }
        double aspectFirst = (double) first.getWidth() / first.getHeight();
        double aspectSecond = (double) second.getWidth() / second.getHeight();
        double smallestAspect = Math.min(aspectFirst, aspectSecond);
        if (Math.abs(aspectFirst - aspectSecond) / smallestAspect
                > properties.getVisual().getAspectTolerance()) {
            return false;
        }
        double areaFirst = (double) first.getWidth() * first.getHeight();
        double areaSecond = (double) second.getWidth() * second.getHeight();
        double maxArea = Math.max(areaFirst, areaSecond);
        double minArea = Math.min(areaFirst, areaSecond);
        return maxArea / minArea <= properties.getVisual().getMaxAreaFactor();
    }

    private void persistGroup(DupGroupCategory category, List<ImageRecord> members,
                              Counters counters) {
        Long scanId = members.get(0).getScan().getId();
        ImageRecord recommended = recommend(members);
        long totalBytes = 0;
        for (ImageRecord member : members) {
            totalBytes += member.getSizeBytes();
        }
        long reclaimable = totalBytes - recommended.getSizeBytes();

        List<Long> memberIds = new ArrayList<>();
        for (ImageRecord member : members) {
            memberIds.add(member.getId());
        }
        Long groupId = updater.saveGroup(scanId, category, recommended.getId(),
                members.size(), reclaimable);
        updater.assignGroup(groupId, memberIds);

        counters.reclaimableBytes += reclaimable;
        if (category == DupGroupCategory.EXACT) {
            counters.exactGroups++;
            counters.exactImages += members.size();
        } else {
            counters.visualGroups++;
            counters.visualImages += members.size();
        }
    }

    /** Suggests the file to keep: highest resolution, then largest, then deterministic path. */
    private ImageRecord recommend(List<ImageRecord> members) {
        ImageRecord best = members.get(0);
        for (int i = 1; i < members.size(); i++) {
            ImageRecord candidate = members.get(i);
            if (isBetter(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean isBetter(ImageRecord candidate, ImageRecord current) {
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

    private long visualArea(ImageRecord record) {
        if (record.getWidth() != null && record.getHeight() != null) {
            return (long) record.getWidth() * record.getHeight();
        }
        return -1L;
    }

    private record Decoded(BufferedImage image, Integer orientation) {
    }

    private static final class VisualGroupData {
        private final ImageRecord representative;
        private final List<ImageRecord> members = new ArrayList<>();

        private VisualGroupData(ImageRecord representative) {
            this.representative = representative;
            this.members.add(representative);
        }
    }

    private static final class Counters {
        private int errors;
        private int exactGroups;
        private int exactImages;
        private int visualGroups;
        private int visualImages;
        private int similarReviewPairs;
        private long reclaimableBytes;
    }
}
