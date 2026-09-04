package com.imagedupmanager.service;

/**
 * User-facing exception raised by file operations (rename, trash, move).
 * Messages are Spanish (ADR D2); mapped to HTTP 400 by the API handler.
 */
public class OperationException extends RuntimeException {

    public OperationException(String message) {
        super(message);
    }

    public OperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
