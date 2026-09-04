package com.imagedupmanager.service;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.ImageStatus;
import com.imagedupmanager.domain.OperationLog;
import com.imagedupmanager.domain.OperationType;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.repository.OperationLogRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * Safe file rename (AGENTS.md #36, Fase 11). Rules:
 * <ul>
 *   <li>Never overwrites another file silently.</li>
 *   <li>Validates Windows file names (characters, reserved names, trailing dot/space).</li>
 *   <li>Keeps the original extension when the user does not type one; changing the
 *       extension is blocked (renaming does not re-encode).</li>
 *   <li>Provides a {@link RenamePreview} for confirmation before executing.</li>
 *   <li>Records every rename in the operation history (reversible).</li>
 * </ul>
 */
@Service
public class RenameService {

    private static final String INVALID_CHARACTERS = "\\/:*?\"<>|";
    private static final Set<String> RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private final ImageRecordRepository imageRecordRepository;
    private final OperationLogRepository operationLogRepository;
    private final ImagePathUpdater imagePathUpdater;

    public RenameService(ImageRecordRepository imageRecordRepository,
                         OperationLogRepository operationLogRepository,
                         ImagePathUpdater imagePathUpdater) {
        this.imageRecordRepository = imageRecordRepository;
        this.operationLogRepository = operationLogRepository;
        this.imagePathUpdater = imagePathUpdater;
    }

    /** Validates a rename and returns the preview (no disk change). */
    public RenamePreview previewRename(Long imageId, String newFileName) {
        ImageRecord record = requireActiveRecord(imageId);
        String newName = resolveNewName(record, newFileName);
        Path source = sourcePath(record);
        Path target = source.resolveSibling(newName);
        ensureNoConflict(target);
        return new RenamePreview(imageId, record.getName(), source.toString(),
                newName, target.toString());
    }

    /** Executes a rename after validating it again (safe against races/conflicts). */
    public void rename(Long imageId, String newFileName) {
        ImageRecord record = requireActiveRecord(imageId);
        String newName = resolveNewName(record, newFileName);
        Path source = sourcePath(record);
        Path target = source.resolveSibling(newName);
        ensureNoConflict(target);

        try {
            Files.move(source, target);
        } catch (IOException e) {
            throw new OperationException("No se ha podido renombrar el archivo.", e);
        }

        imagePathUpdater.updatePathAndName(imageId, target.toString(), newName);
        operationLogRepository.save(new OperationLog(OperationType.RENAME, imageId,
                source.toString(), target.toString(), LocalDateTime.now()));
    }

    private ImageRecord requireActiveRecord(Long imageId) {
        ImageRecord record = imageRecordRepository.findById(imageId)
                .orElseThrow(() -> new OperationException("No se ha encontrado la imagen solicitada."));
        if (record.getStatus() != ImageStatus.ACTIVE) {
            throw new OperationException("La imagen no está activa y no puede renombrarse.");
        }
        return record;
    }

    private Path sourcePath(ImageRecord record) {
        Path path = Paths.get(record.getAbsolutePath());
        if (!Files.isRegularFile(path)) {
            throw new OperationException("El archivo de la imagen ya no está disponible en el disco.");
        }
        return path;
    }

    private void ensureNoConflict(Path target) {
        if (Files.exists(target)) {
            throw new OperationException(
                    "Ya existe un archivo con el nombre elegido: " + target.getFileName());
        }
    }

    /** Validates the requested name and returns it with the original extension kept. */
    private String resolveNewName(ImageRecord record, String newFileName) {
        if (newFileName == null || newFileName.trim().isEmpty()) {
            throw new OperationException("El nuevo nombre no puede estar vacío.");
        }
        String requested = newFileName.trim();
        if (requested.equals(record.getName())) {
            throw new OperationException("El nuevo nombre es igual al actual.");
        }
        validateWindowsFileName(requested);

        String extension = record.getExtension() == null
                ? "" : record.getExtension().toLowerCase(Locale.ROOT);
        int dot = requested.lastIndexOf('.');
        if (dot < 0) {
            if (extension.isEmpty()) {
                throw new OperationException("No se ha podido determinar la extensión del archivo.");
            }
            return requested + "." + extension;
        }
        String requestedExtension = requested.substring(dot + 1);
        if (requestedExtension.isEmpty()) {
            throw new OperationException("El nombre no puede terminar en un punto.");
        }
        if (!extension.isEmpty() && !requestedExtension.equalsIgnoreCase(extension)) {
            throw new OperationException(
                    "No se puede cambiar la extensión del archivo (solo el nombre).");
        }
        return requested;
    }

    private void validateWindowsFileName(String fileName) {
        for (int i = 0; i < fileName.length(); i++) {
            char character = fileName.charAt(i);
            if (character < 32 || INVALID_CHARACTERS.indexOf(character) >= 0) {
                throw new OperationException(
                        "El nombre contiene caracteres no permitidos en Windows.");
            }
        }
        if (fileName.endsWith(".") || fileName.endsWith(" ")) {
            throw new OperationException("El nombre no puede terminar en punto o espacio.");
        }
        int dot = fileName.indexOf('.');
        String base = (dot < 0 ? fileName : fileName.substring(0, dot)).toUpperCase(Locale.ROOT);
        if (RESERVED_NAMES.contains(base)) {
            throw new OperationException("Ese nombre está reservado por Windows y no puede usarse.");
        }
    }
}
