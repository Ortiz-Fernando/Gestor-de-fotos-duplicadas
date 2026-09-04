package com.imagedupmanager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Single {@link FileTrash} bean. Sends the file to the Windows Recycle Bin when the
 * volume supports it, otherwise to the application internal trash folder. Never deletes
 * permanently. Fails closed: if recycle-bin availability cannot be determined, the file
 * is moved to the internal trash (ADR D10).
 */
@Component
public class FileTrashDelegator implements FileTrash {

    private final RecycleBinSupport recycleBinSupport;
    private final FileTrash windowsFileTrash;
    private final FileTrash internalFileTrash;

    @Autowired
    public FileTrashDelegator(RecycleBinSupport recycleBinSupport) {
        this(recycleBinSupport, new WindowsFileTrash(), new InternalFileTrash());
    }

    FileTrashDelegator(RecycleBinSupport recycleBinSupport, FileTrash windowsFileTrash,
                       FileTrash internalFileTrash) {
        this.recycleBinSupport = recycleBinSupport;
        this.windowsFileTrash = windowsFileTrash;
        this.internalFileTrash = internalFileTrash;
    }

    @Override
    public Path sendToTrash(Path file) {
        if (recycleBinSupport.supportsRecycleBin(file)) {
            return windowsFileTrash.sendToTrash(file);
        }
        return internalFileTrash.sendToTrash(file);
    }
}
