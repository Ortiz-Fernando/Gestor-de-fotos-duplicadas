package com.imagedupmanager.hashing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 hashing by streaming (AGENTS.md #16).
 *
 * <p>Files are never loaded fully into memory: they are read with an 8 MB buffer.
 * The returned digest is the standard lowercase hexadecimal form (64 characters),
 * which is also the format persisted in {@code ImageRecord.sha256}.
 */
public final class Sha256Hasher {

    public static final int BUFFER_SIZE = 8 * 1024 * 1024;

    private static final String ALGORITHM = "SHA-256";

    private Sha256Hasher() {
    }

    /** Computes the SHA-256 hex digest of the given file, streaming it from disk. */
    public static String of(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return of(in);
        } catch (IOException e) {
            throw new HashingException("No se ha podido calcular el SHA-256 del archivo: " + path, e);
        }
    }

    /** Computes the SHA-256 hex digest of the given input stream. */
    public static String of(InputStream input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new HashingException("No se ha podido calcular el SHA-256.", e);
        }
    }
}
