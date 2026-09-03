package com.imagedupmanager.service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Image formats handled by the application (ADR D4).
 *
 * <ul>
 *   <li>Fully analysable (SHA-256 + perceptual hash): JPG/JPEG, PNG, GIF, BMP, WEBP, TIFF.</li>
 *   <li>Exact detection only (SHA-256, no perceptual hash in v1): HEIC/HEIF/RAW.</li>
 * </ul>
 */
public final class SupportedImageFormats {

    private static final Set<String> VISUALLY_ANALYSABLE = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tif", "tiff");

    private static final Set<String> EXACT_ONLY = Set.of(
            "heic", "heif",
            // Common RAW camera formats
            "cr2", "cr3", "nef", "arw", "dng", "orf", "rw2", "pef", "raf", "srw", "x3f");

    private static final Set<String> ALL =
            Stream.concat(VISUALLY_ANALYSABLE.stream(), EXACT_ONLY.stream())
                    .collect(Collectors.toUnmodifiableSet());

    private SupportedImageFormats() {
    }

    /** Returns the lowercase extension of the given file name, if present. */
    public static Optional<String> extensionOf(String fileName) {
        if (fileName == null) {
            return Optional.empty();
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT));
    }

    /** True when the file name has an image extension handled by the application. */
    public static boolean isImageFile(String fileName) {
        return extensionOf(fileName).map(ALL::contains).orElse(false);
    }

    /** True when the (lowercase) extension can be visually analysed (pHash) in v1. */
    public static boolean isVisuallyAnalysable(String extension) {
        return extension != null && VISUALLY_ANALYSABLE.contains(extension.toLowerCase(Locale.ROOT));
    }
}
