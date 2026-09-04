package com.imagedupmanager.web.dto;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.ImageStatus;
import com.imagedupmanager.domain.Scan;
import com.imagedupmanager.domain.ScanStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for the REST API. Nested records keep the number of files small; Jackson supports
 * static nested records for both serialization and deserialization.
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    /** Request body for POST /api/scans. */
    public record StartScanRequest(
            @NotBlank(message = "Debes indicar la ruta de la carpeta a analizar.")
            @Size(max = 1024, message = "La ruta indicada es demasiado larga.")
            String rootPath) {
    }

    /** Response for POST /api/scans. */
    public record CreateScanResponse(Long id, ScanStatus status) {
    }

    public record ScanSummary(Long id, String rootPath, ScanStatus status,
                              LocalDateTime startedAt, LocalDateTime finishedAt,
                              int fileCount, int errorCount, String errorMessage) {

        public static ScanSummary from(Scan scan) {
            return new ScanSummary(scan.getId(), scan.getRootPath(), scan.getStatus(),
                    scan.getStartedAt(), scan.getFinishedAt(), scan.getFileCount(),
                    scan.getErrorCount(), scan.getErrorMessage());
        }
    }

    public record GroupSummary(Long id, Long scanId, String category,
                               Long recommendedImageId, int memberCount,
                               long reclaimableBytes) {
    }

    public record GroupDetail(Long id, Long scanId, String category,
                              Long recommendedImageId, int memberCount,
                              long reclaimableBytes, List<Image> members) {
    }

    public record Image(Long id, String absolutePath, String name, String folder,
                        String extension, long sizeBytes, LocalDateTime lastModified,
                        String sha256, Long phash, Integer width, Integer height,
                        Integer exifOrientation, ImageStatus status) {

        public static Image from(ImageRecord record) {
            return new Image(record.getId(), record.getAbsolutePath(), record.getName(),
                    record.getFolder(), record.getExtension(), record.getSizeBytes(),
                    record.getLastModified(), record.getSha256(), record.getPhash(),
                    record.getWidth(), record.getHeight(), record.getExifOrientation(),
                    record.getStatus());
        }
    }

    /** Operation history entry. */
    public record Operation(Long id, String type, Long imageId, String sourcePath,
                            String destinationPath, LocalDateTime operationTime,
                            boolean reversible, LocalDateTime undoneAt) {

        public static Operation from(com.imagedupmanager.domain.OperationLog log) {
            return new Operation(log.getId(), log.getType().name(), log.getImageId(),
                    log.getSourcePath(), log.getDestinationPath(), log.getOperationTime(),
                    log.isReversible(), log.getUndoneAt());
        }
    }

    /** Request body for image operations that use a file name. */
    public record RenameRequest(
            @NotBlank(message = "Indica el nuevo nombre del archivo.")
            @Size(max = 255, message = "El nuevo nombre es demasiado largo.")
            String newName) {
    }

    /** Trash requires explicit user confirmation (never automatic). */
    public record TrashRequest(
            @NotNull(message = "Debes confirmar el envío a la Papelera.")
            @AssertTrue(message = "Debes confirmar el envío a la Papelera.")
            Boolean confirm) {
    }

    public record ApiError(LocalDateTime timestamp, int status, String error, String message) {

        public static ApiError of(int status, String error, String message) {
            return new ApiError(LocalDateTime.now(), status, error, message);
        }
    }
}
