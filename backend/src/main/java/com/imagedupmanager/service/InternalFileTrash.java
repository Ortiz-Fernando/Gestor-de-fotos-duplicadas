package com.imagedupmanager.service;

import com.imagedupmanager.config.FileSystemBootstrap;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Application-managed trash folder ({@code data/trash/}) used as a safe fallback when the
 * volume has no operating system Recycle Bin (e.g. removable USB drives, ADR D10). Moves
 * the file preserving its content and never deletes it permanently.
 */
public class InternalFileTrash implements FileTrash {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_ATTEMPTS = 50;

    private final Path trashRoot;

    public InternalFileTrash() {
        this(Path.of(FileSystemBootstrap.DATA_DIR, "trash"));
    }

    public InternalFileTrash(Path trashRoot) {
        this.trashRoot = trashRoot;
    }

    @Override
    public Path sendToTrash(Path file) {
        Path absolute = file.toAbsolutePath();
        if (!Files.isRegularFile(absolute)) {
            throw new OperationException(
                    "El archivo ya no está disponible y no puede enviarse a la papelera.");
        }
        try {
            Files.createDirectories(trashRoot);
        } catch (IOException e) {
            throw new OperationException(
                    "No se ha podido preparar la papelera interna de la aplicación.", e);
        }
        String stamp = LocalDateTime.now().format(STAMP);
        String originalName = absolute.getFileName().toString();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            Path target = trashRoot.resolve(stamp + "-" + suffix + "-" + originalName);
            try {
                Files.move(absolute, target);
                return target;
            } catch (FileAlreadyExistsException e) {
                // Extremely unlikely collision: retry with a new random suffix.
            } catch (IOException e) {
                throw new OperationException(
                        "No se ha podido mover el archivo a la papelera interna de la aplicación.", e);
            }
        }
        throw new OperationException(
                "No se ha podido generar un nombre único para la papelera interna.");
    }
}
