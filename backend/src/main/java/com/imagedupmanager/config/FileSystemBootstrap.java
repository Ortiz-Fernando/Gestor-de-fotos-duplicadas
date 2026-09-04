package com.imagedupmanager.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates the local data directories (database + thumbnails cache) before the Spring
 * context starts. Paths are relative to the run directory ("backend/" during development
 * -> the "data/" folder at the repository root). Packaging (Fase 15) may override this.
 */
public final class FileSystemBootstrap {

    /** Base data directory, relative to the backend run directory. */
    public static final String DATA_DIR = "../data";

    private FileSystemBootstrap() {
    }

    public static void ensureDataDirectories() {
        createDirectory(Path.of(DATA_DIR, "database"));
        createDirectory(Path.of(DATA_DIR, "thumbnails"));
        createDirectory(Path.of(DATA_DIR, "trash"));
    }

    private static void createDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se ha podido crear el directorio de datos: " + directory.toAbsolutePath(), e);
        }
    }
}
