package com.imagedupmanager.service;

import com.imagedupmanager.domain.OperationLog;
import com.imagedupmanager.domain.OperationType;
import com.imagedupmanager.repository.OperationLogRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Operation history and undo (AGENTS.md #38). Undo is only possible when the operation was
 * recorded as reversible (currently renames). Trash operations must be restored manually
 * from the Windows Recycle Bin and are clearly reported as non-reversible.
 */
@Service
public class OperationService {

    private final OperationLogRepository operationLogRepository;
    private final ImagePathUpdater imagePathUpdater;

    public OperationService(OperationLogRepository operationLogRepository,
                            ImagePathUpdater imagePathUpdater) {
        this.operationLogRepository = operationLogRepository;
        this.imagePathUpdater = imagePathUpdater;
    }

    public List<OperationLog> history() {
        return operationLogRepository.findAllByOrderByOperationTimeDesc();
    }

    public void undo(Long operationId) {
        OperationLog log = operationLogRepository.findById(operationId)
                .orElseThrow(() -> new OperationException("No se ha encontrado la operación."));
        if (log.getUndoneAt() != null) {
            throw new OperationException("La operación ya fue deshecha.");
        }
        switch (log.getType()) {
            case RENAME -> undoRename(log);
            case TRASH -> throw new OperationException(
                    "No se puede deshacer automáticamente el envío a la Papelera. "
                            + "Restaura el archivo desde la Papelera de Windows.");
            default -> throw new OperationException("Esta operación no se puede deshacer.");
        }
    }

    private void undoRename(OperationLog log) {
        if (!log.isReversible()) {
            throw new OperationException("Esta operación no se puede deshacer.");
        }
        Path source = Path.of(log.getSourcePath());
        Path destination = Path.of(log.getDestinationPath());
        if (!Files.isRegularFile(destination)) {
            throw new OperationException(
                    "No se puede deshacer: el archivo ya no existe en su nueva ubicación.");
        }
        if (Files.exists(source)) {
            throw new OperationException(
                    "No se puede deshacer: ya existe un archivo en la ubicación original.");
        }
        try {
            Files.move(destination, source);
        } catch (IOException e) {
            throw new OperationException("No se ha podido deshacer el renombrado.", e);
        }

        if (log.getImageId() != null) {
            imagePathUpdater.updatePathAndName(log.getImageId(), source.toString(),
                    source.getFileName().toString());
        }
        log.setUndoneAt(LocalDateTime.now());
        operationLogRepository.save(log);

        OperationLog undoLog = new OperationLog(OperationType.UNDO, log.getImageId(),
                log.getSourcePath(), log.getDestinationPath(), LocalDateTime.now());
        undoLog.setReversible(false);
        operationLogRepository.save(undoLog);
    }
}
