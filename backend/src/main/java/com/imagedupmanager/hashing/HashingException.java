package com.imagedupmanager.hashing;

/**
 * Technical exception raised while computing file hashes.
 */
public class HashingException extends RuntimeException {

    public HashingException(String message) {
        super(message);
    }

    public HashingException(String message, Throwable cause) {
        super(message, cause);
    }
}
