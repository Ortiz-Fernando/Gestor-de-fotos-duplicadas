package com.imagedupmanager.service;

import java.nio.file.Path;

/**
 * Abstraction for moving files to the operating system Recycle Bin / trash.
 */
public interface FileTrash {

    /**
     * Sends the given file to the trash. Implementations must never delete permanently.
     *
     * @return the path where the file was stored when it was moved to the application
     *         internal trash, or {@code null} when it was sent to the operating system
     *         Recycle Bin / trash
     * @throws OperationException when the operation fails or the platform is not supported
     */
    Path sendToTrash(Path file);
}
