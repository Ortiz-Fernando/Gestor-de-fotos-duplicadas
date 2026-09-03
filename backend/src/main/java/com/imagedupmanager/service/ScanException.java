package com.imagedupmanager.service;

/**
 * User-facing exception raised by file system scanning operations.
 * Messages are Spanish because they are shown directly to the user (ADR D2).
 */
public class ScanException extends RuntimeException {

    public ScanException(String message) {
        super(message);
    }

    public ScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
