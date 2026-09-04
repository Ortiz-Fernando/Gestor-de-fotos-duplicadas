package com.imagedupmanager.service;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.ImageStatus;
import com.imagedupmanager.domain.OperationLog;
import com.imagedupmanager.domain.OperationType;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.repository.OperationLogRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Sends image files to the OS Recycle Bin (never a permanent delete). The image record is
 * marked as IN_TRASH, removed from its duplicate group and the operation is recorded as
 * non-reversible automatically (restore happens manually from the Windows Recycle Bin or
 * from the application internal trash folder when the volume has no Recycle Bin).
 */
@Service
public class DeleteService {

    private final ImageRecordRepository imageRecordRepository;
    private final OperationLogRepository operationLogRepository;
    private final GroupService groupService;
    private final FileTrash fileTrash;

    public DeleteService(ImageRecordRepository imageRecordRepository,
                         OperationLogRepository operationLogRepository,
                         GroupService groupService,
                         FileTrash fileTrash) {
        this.imageRecordRepository = imageRecordRepository;
        this.operationLogRepository = operationLogRepository;
        this.groupService = groupService;
        this.fileTrash = fileTrash;
    }

    public void sendToTrash(Long imageId) {
        ImageRecord record = imageRecordRepository.findById(imageId)
                .orElseThrow(() -> new OperationException("No se ha encontrado la imagen solicitada."));
        if (record.getStatus() != ImageStatus.ACTIVE) {
            throw new OperationException("La imagen no está activa y no puede enviarse a la Papelera.");
        }
        Path path = Paths.get(record.getAbsolutePath());
        if (!Files.isRegularFile(path)) {
            throw new OperationException("El archivo de la imagen ya no está disponible en el disco.");
        }

        Path destination = fileTrash.sendToTrash(path);
        groupService.markTrashedAndRefresh(imageId);

        OperationLog log = new OperationLog(OperationType.TRASH, imageId,
                path.toString(),
                destination != null ? destination.toString() : null,
                LocalDateTime.now());
        log.setReversible(false);
        operationLogRepository.save(log);
    }
}
