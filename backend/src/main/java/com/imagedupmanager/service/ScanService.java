package com.imagedupmanager.service;

import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.Scan;
import com.imagedupmanager.domain.ScanStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recursive file system scanner for image collections.
 *
 * <p>Behaviour (AGENTS.md #14): recursive walk, does not follow symbolic links, tolerates
 * files that disappear during the walk (they are logged as errors, never fatal), reports
 * progress in memory and supports cooperative cancellation. Files are persisted in
 * batches to avoid loading whole collections into memory.
 */
@Service
public class ScanService {

    private static final int BATCH_SIZE = 250;

    private final ScanPersister persister;
    private final TaskExecutor scanExecutor;
    private final Map<Long, MutableScanState> runningScans = new ConcurrentHashMap<>();

    public ScanService(ScanPersister persister,
                       @Qualifier("scanTaskExecutor") TaskExecutor scanExecutor) {
        this.persister = persister;
        this.scanExecutor = scanExecutor;
    }

    /**
     * Runs a scan synchronously and returns the persisted, completed {@link Scan}.
     * Mainly used by tests and internal callers.
     */
    public Scan scanSync(Path rootPath) {
        Path root = validateRoot(rootPath);
        Scan scan = persister.create(new Scan(root.toString(), LocalDateTime.now()));
        return runScan(scan, root, new MutableScanState(scan.getId()));
    }

    /** Starts a scan asynchronously on the scan executor and returns the scan id. */
    public Long scanAsync(Path rootPath) {
        Path root = validateRoot(rootPath);
        Scan scan = persister.create(new Scan(root.toString(), LocalDateTime.now()));
        MutableScanState state = new MutableScanState(scan.getId());
        runningScans.put(scan.getId(), state);
        try {
            scanExecutor.execute(() -> {
                try {
                    runScan(scan, root, state);
                } catch (Exception e) {
                    // Final safety net; unexpected failure still recorded in the database.
                    state.setStatus(ScanStatus.FAILED);
                    persister.save(scan);
                }
            });
        } catch (RuntimeException e) {
            runningScans.remove(scan.getId());
            throw e;
        }
        return scan.getId();
    }

    /** Requests cancellation of a running scan (cooperative flag). */
    public void cancel(Long scanId) {
        MutableScanState state = runningScans.get(scanId);
        if (state != null) {
            state.cancel();
        }
    }

    /** Returns the current progress snapshot for an asynchronous scan, if present. */
    public Optional<ScanProgress> getProgress(Long scanId) {
        MutableScanState state = runningScans.get(scanId);
        return state == null ? Optional.empty() : Optional.of(state.snapshot());
    }

    private Path validateRoot(Path rootPath) {
        if (rootPath == null) {
            throw new ScanException("Debes seleccionar una carpeta para analizar.");
        }
        Path root = rootPath.toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            throw new ScanException("No se ha podido acceder a la carpeta seleccionada.");
        }
        if (!Files.isDirectory(root)) {
            throw new ScanException("La ruta seleccionada no es una carpeta.");
        }
        if (!Files.isReadable(root)) {
            throw new ScanException("No se tienen permisos para leer la carpeta seleccionada.");
        }
        return root;
    }

    private Scan runScan(Scan scan, Path root, MutableScanState state) {
        List<ImageRecord> pending = new ArrayList<>();
        try {
            Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
                    new ScanFileVisitor(root, state, pending));
            flush(pending, state);
            return finish(scan, state, ScanStatus.COMPLETED, null);
        } catch (java.util.concurrent.CancellationException cancellation) {
            flushQuietly(pending, state);
            return finish(scan, state, ScanStatus.CANCELLED,
                    "El análisis fue cancelado por el usuario.");
        } catch (ScanException scanFailure) {
            flushQuietly(pending, state);
            return finish(scan, state, ScanStatus.FAILED, scanFailure.getMessage());
        } catch (Exception unexpected) {
            flushQuietly(pending, state);
            return finish(scan, state, ScanStatus.FAILED,
                    "No se ha podido completar el análisis de la carpeta.");
        }
    }

    private Scan finish(Scan scan, MutableScanState state, ScanStatus status, String errorMessage) {
        scan.setStatus(status);
        scan.setFinishedAt(LocalDateTime.now());
        scan.setFileCount(state.storedFiles());
        scan.setErrorCount(state.errorCount());
        scan.setErrorMessage(errorMessage);
        // Persist BEFORE exposing the terminal state: callers may read the database as
        // soon as the progress shows a terminal status.
        Scan saved = persister.save(scan);
        state.setStatus(status);
        return saved;
    }

    private void flush(List<ImageRecord> pending, MutableScanState state) {
        if (pending.isEmpty()) {
            return;
        }
        persister.saveImageBatch(state.scanId(), new ArrayList<>(pending));
        state.addStoredFiles(pending.size());
        pending.clear();
    }

    private void flushQuietly(List<ImageRecord> pending, MutableScanState state) {
        try {
            flush(pending, state);
        } catch (RuntimeException ignored) {
            // Best effort only: the scan is already ending abnormally.
        }
    }

    /**
     * File visitor for a scan. Recurses directories, ignores non-image files, does not
     * follow symbolic links and never aborts on per-file/per-directory access failures
     * (only a failure to read the scan root is treated as fatal).
     */
    private final class ScanFileVisitor extends java.nio.file.SimpleFileVisitor<Path> {

        private final Path root;
        private final MutableScanState state;
        private final List<ImageRecord> pending;

        private ScanFileVisitor(Path root, MutableScanState state, List<ImageRecord> pending) {
            this.root = root;
            this.state = state;
            this.pending = pending;
        }

        @Override
        public java.nio.file.FileVisitResult preVisitDirectory(
                Path dir, java.nio.file.attribute.BasicFileAttributes attrs) {
            if (state.isCancelled()) {
                throw new java.util.concurrent.CancellationException();
            }
            if (Files.isSymbolicLink(dir)) {
                return java.nio.file.FileVisitResult.SKIP_SUBTREE;
            }
            return java.nio.file.FileVisitResult.CONTINUE;
        }

        @Override
        public java.nio.file.FileVisitResult visitFile(
                Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
            if (state.isCancelled()) {
                throw new java.util.concurrent.CancellationException();
            }
            state.addDiscoveredFile();
            state.setCurrentPath(file.toString());

            if (attrs.isRegularFile() && !attrs.isSymbolicLink()) {
                String fileName = file.getFileName().toString();
                Optional<String> extension = SupportedImageFormats.extensionOf(fileName);
                if (extension.isPresent() && SupportedImageFormats.isImageFile(fileName)) {
                    Path folder = file.getParent();
                    ImageRecord record = new ImageRecord(null, file.toString(), fileName,
                            folder == null ? null : folder.toString(), extension.get(),
                            attrs.size(), toUtcLocalDateTime(attrs));
                    record.setAnalysable(SupportedImageFormats.isVisuallyAnalysable(extension.get()));
                    pending.add(record);
                    if (pending.size() >= BATCH_SIZE) {
                        flush(pending, state);
                    }
                }
            }
            return java.nio.file.FileVisitResult.CONTINUE;
        }

        @Override
        public java.nio.file.FileVisitResult visitFileFailed(
                Path file, java.io.IOException exc) {
            state.addError();
            if (file.equals(root)) {
                // Losing access to the scan root (e.g. disconnected drive) is fatal.
                throw new ScanException("No se ha podido acceder a la carpeta seleccionada.", exc);
            }
            return java.nio.file.FileVisitResult.CONTINUE;
        }
    }

    private static java.time.LocalDateTime toUtcLocalDateTime(
            java.nio.file.attribute.BasicFileAttributes attrs) {
        return java.time.LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), java.time.ZoneOffset.UTC);
    }

    /** Mutable, thread-safe progress state of one scan. */
    private static final class MutableScanState {

        private final Long scanId;
        private final java.util.concurrent.atomic.AtomicInteger discoveredFiles =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger storedFiles =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger errorCount =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicReference<ScanStatus> status =
                new java.util.concurrent.atomic.AtomicReference<>(ScanStatus.RUNNING);
        private final java.util.concurrent.atomic.AtomicReference<String> currentPath =
                new java.util.concurrent.atomic.AtomicReference<>();
        private final java.util.concurrent.atomic.AtomicBoolean cancelled =
                new java.util.concurrent.atomic.AtomicBoolean();

        private MutableScanState(Long scanId) {
            this.scanId = scanId;
        }

        Long scanId() {
            return scanId;
        }

        boolean isCancelled() {
            return cancelled.get();
        }

        void cancel() {
            cancelled.set(true);
        }

        void addDiscoveredFile() {
            discoveredFiles.incrementAndGet();
        }

        void addStoredFiles(int count) {
            storedFiles.addAndGet(count);
        }

        void addError() {
            errorCount.incrementAndGet();
        }

        int storedFiles() {
            return storedFiles.get();
        }

        int errorCount() {
            return errorCount.get();
        }

        void setCurrentPath(String path) {
            currentPath.set(path);
        }

        void setStatus(ScanStatus newStatus) {
            status.set(newStatus);
        }

        ScanProgress snapshot() {
            return new ScanProgress(scanId, status.get(), discoveredFiles.get(),
                    storedFiles.get(), errorCount.get(), currentPath.get());
        }
    }
}
