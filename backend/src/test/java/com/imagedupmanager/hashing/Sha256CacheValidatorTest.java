package com.imagedupmanager.hashing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the SHA-256 cache reuse policy (AGENTS.md #17).
 */
class Sha256CacheValidatorTest {

    @TempDir
    Path tempDir;

    private LocalDateTime lastModifiedUtc(Path file) throws IOException {
        Instant instant = Files.getLastModifiedTime(file).toInstant();
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Test
    void unchangedFileCanReuseCachedDigest() throws IOException {
        Path file = Files.write(tempDir.resolve("foto.jpg"), "foto".getBytes());
        long size = Files.size(file);
        LocalDateTime lastModified = lastModifiedUtc(file);

        assertTrue(Sha256CacheValidator.isUsable(size, lastModified, file));
    }

    @Test
    void changedSizeInvalidatesCache() throws IOException {
        Path file = Files.write(tempDir.resolve("foto.jpg"), "foto".getBytes());
        LocalDateTime lastModified = lastModifiedUtc(file);

        Files.write(file, "foto con más contenido".getBytes());
        assertFalse(Sha256CacheValidator.isUsable(Files.size(file) - 10, lastModified, file));
    }

    @Test
    void changedModificationTimeInvalidatesCache() throws IOException {
        Path file = Files.write(tempDir.resolve("foto.jpg"), "mismo-tamaño".getBytes());
        long size = Files.size(file);
        LocalDateTime original = lastModifiedUtc(file);

        // Keep the same size but force a different last-modified timestamp.
        Files.setLastModifiedTime(file, FileTime.fromMillis(
                original.toInstant(ZoneOffset.UTC).toEpochMilli() + 60_000));
        assertFalse(Sha256CacheValidator.isUsable(size, original, file));
    }

    @Test
    void nullArgumentsAreNeverUsable() throws IOException {
        Path file = Files.write(tempDir.resolve("foto.jpg"), "foto".getBytes());
        long size = Files.size(file);
        LocalDateTime lastModified = lastModifiedUtc(file);

        assertFalse(Sha256CacheValidator.isUsable(null, lastModified, file));
        assertFalse(Sha256CacheValidator.isUsable(size, null, file));
        assertFalse(Sha256CacheValidator.isUsable(size, lastModified, null));
    }
}
