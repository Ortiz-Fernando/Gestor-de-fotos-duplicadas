package com.imagedupmanager.hashing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Safe reuse policy for cached SHA-256 digests (AGENTS.md #17).
 *
 * <p>A cached digest may be reused only when BOTH the file size and the last modified
 * timestamp still match the values recorded when the digest was computed. Any other use
 * could produce an incorrect content identification, which is never acceptable.
 */
public final class Sha256CacheValidator {

    private Sha256CacheValidator() {
    }

    /**
     * Returns true when the cached digest (recorded with the given size and UTC last
     * modified time) can still be trusted for the current state of {@code file}.
     */
    public static boolean isUsable(Long cachedSizeBytes, LocalDateTime cachedLastModifiedUtc, Path file) {
        if (cachedSizeBytes == null || cachedLastModifiedUtc == null || file == null) {
            return false;
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            if (!attributes.isRegularFile()) {
                return false;
            }
            if (attributes.size() != cachedSizeBytes) {
                return false;
            }
            Instant cachedInstant = cachedLastModifiedUtc.toInstant(ZoneOffset.UTC);
            return attributes.lastModifiedTime().toInstant().equals(cachedInstant);
        } catch (IOException e) {
            return false;
        }
    }
}
