package com.mathdep.service.document;

import org.springframework.http.HttpStatus;

public class DocumentTextExtractionException extends RuntimeException {

    private final HttpStatus status;

    public DocumentTextExtractionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public DocumentTextExtractionException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
