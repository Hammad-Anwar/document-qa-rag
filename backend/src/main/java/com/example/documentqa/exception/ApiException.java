package com.example.documentqa.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String error;
    private final UUID documentId;

    public ApiException(HttpStatus status, String error, String message) {
        this(status, error, message, null);
    }
    public ApiException(HttpStatus status, String error, String message, UUID documentId) {
        super(message);
        this.status = status;
        this.error = error;
        this.documentId = documentId;
    }
    public HttpStatus status() { return status; }
    public String error() { return error; }
    public UUID documentId() { return documentId; }
}
