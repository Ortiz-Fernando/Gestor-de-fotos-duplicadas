package com.imagedupmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FileTrashDelegator}: it must choose the Windows Recycle Bin only when
 * the volume supports it and fall back to the internal trash otherwise (ADR D10). The
 * native detection is mocked so tests never touch the real Recycle Bin.
 */
class FileTrashDelegatorTest {

    @TempDir
    Path tempDir;

    @Test
    void usesInternalTrashWhenVolumeHasNoRecycleBin() throws IOException {
        RecycleBinSupport support = mock(RecycleBinSupport.class);
        FileTrash windowsTrash = mock(FileTrash.class);
        when(support.supportsRecycleBin(any(Path.class))).thenReturn(false);
        Path trashRoot = tempDir.resolve("trash");
        FileTrashDelegator delegator =
                new FileTrashDelegator(support, windowsTrash, new InternalFileTrash(trashRoot));

        Path origin = Files.createDirectories(tempDir.resolve("origen"));
        Path file = origin.resolve("foto.jpg");
        Files.write(file, "contenido".getBytes(StandardCharsets.UTF_8));

        Path destination = delegator.sendToTrash(file);

        assertNotNull(destination, "la papelera interna debe devolver la ruta de destino");
        assertTrue(Files.isRegularFile(destination));
        assertEquals("contenido", Files.readString(destination));
        assertFalse(Files.exists(file), "el original no debe permanecer en su ubicación");
        verify(windowsTrash, never()).sendToTrash(any(Path.class));
    }

    @Test
    void delegatesToWindowsRecycleBinWhenVolumeSupportsIt() throws IOException {
        RecycleBinSupport support = mock(RecycleBinSupport.class);
        FileTrash windowsTrash = mock(FileTrash.class);
        Path origin = Files.createDirectories(tempDir.resolve("origen"));
        Path file = origin.resolve("foto.jpg");
        Files.write(file, "x".getBytes(StandardCharsets.UTF_8));
        when(support.supportsRecycleBin(file)).thenReturn(true);
        when(windowsTrash.sendToTrash(file)).thenReturn(null);

        FileTrashDelegator delegator = new FileTrashDelegator(
                support, windowsTrash, new InternalFileTrash(tempDir.resolve("trash")));

        Path destination = delegator.sendToTrash(file);

        assertNull(destination, "la Papelera del sistema no devuelve ubicación interna");
        verify(windowsTrash).sendToTrash(file);
    }
}
