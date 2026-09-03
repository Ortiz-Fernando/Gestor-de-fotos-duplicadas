package com.imagedupmanager.hashing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the streaming SHA-256 hasher (Fase 5).
 */
class Sha256HasherTest {

    @TempDir
    Path tempDir;

    @Test
    void emptyFileProducesKnownSha256() throws IOException {
        Path file = Files.write(tempDir.resolve("vacio.bin"), new byte[0]);
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Sha256Hasher.of(file));
    }

    @Test
    void identicalFilesProduceSameDigest() throws IOException {
        byte[] content = "misma-fotografia-copy".getBytes();
        Path a = Files.write(tempDir.resolve("a.jpg"), content);
        Path b = Files.write(tempDir.resolve("b.jpg"), content);

        assertEquals(Sha256Hasher.of(a), Sha256Hasher.of(b));
        assertEquals(Sha256Hasher.of(a), Sha256Hasher.of(new ByteArrayInputStream(content)));
    }

    @Test
    void differentFilesProduceDifferentDigests() throws IOException {
        Path a = Files.write(tempDir.resolve("a.jpg"), "contenido uno".getBytes());
        Path b = Files.write(tempDir.resolve("b.jpg"), "contenido dos!!!".getBytes());
        assertNotEquals(Sha256Hasher.of(a), Sha256Hasher.of(b));
    }

    @Test
    void largeFileIsStreamedCorrectlyBeyondBuffer() throws IOException, NoSuchAlgorithmException {
        Path large = tempDir.resolve("grande.bin");
        byte[] payload = new byte[20 * 1024 * 1024]; // > 8 MB buffer
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) ((i * 31 + 7) & 0xff);
        }
        Files.write(large, payload);

        String actual = Sha256Hasher.of(large);

        MessageDigest manual = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(large)) {
            byte[] chunk = new byte[1024];
            int read;
            while ((read = in.read(chunk)) != -1) {
                manual.update(chunk, 0, read);
            }
        }
        assertEquals(HexFormat.of().formatHex(manual.digest()), actual);
    }

    @Test
    void missingFileThrowsHashingException() {
        Path missing = tempDir.resolve("no-existe.jpg");
        assertThrows(HashingException.class, () -> Sha256Hasher.of(missing));
    }
}
