package com.imagedupmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the application internal trash ({@link InternalFileTrash}, ADR D10). The
 * implementation must move the file (never delete it) and preserve its content.
 */
class InternalFileTrashTest {

    @TempDir
    Path tempDir;

    @Test
    void movesFilePreservingContentUnderTrashRoot() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("raiz"));
        Path file = root.resolve("foto.jpg");
        byte[] content = "datos de la foto".getBytes(StandardCharsets.UTF_8);
        Files.write(file, content);
        Path trashRoot = tempDir.resolve("trash");

        Path destination = new InternalFileTrash(trashRoot).sendToTrash(file);

        assertFalse(Files.exists(file), "el archivo original debe desaparecer tras moverse");
        assertTrue(Files.isRegularFile(destination), "el archivo debe existir en la papelera");
        assertTrue(destination.getParent().equals(trashRoot),
                "el archivo debe estar bajo la raíz de la papelera interna");
        assertTrue(destination.getFileName().toString().endsWith("-foto.jpg"),
                "el nombre original debe conservarse");
        assertArrayEquals(content, Files.readAllBytes(destination),
                "el contenido debe conservarse íntegro");
    }

    @Test
    void missingFileIsRejected() {
        Path missing = tempDir.resolve("no-existe.jpg");

        assertThrows(OperationException.class,
                () -> new InternalFileTrash(tempDir.resolve("trash")).sendToTrash(missing));
    }
}
