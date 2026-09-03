package com.imagedupmanager.web;

/**
 * Thrown when a requested resource does not exist. Mapped to HTTP 404 by the API handler.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
