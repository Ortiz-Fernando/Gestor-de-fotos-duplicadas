package com.imagedupmanager.service;

import java.nio.file.Path;

/**
 * Abstraction for moving files to the operating system Recycle Bin / trash.
 */
public interface FileTrash {

    /**
     * Sends the given file to the trash. Implementations must never delete permanently.
     *
     * @throws OperationException when the operation fails or the platform is not supported
     */
    void sendToTrash(Path file);
}
