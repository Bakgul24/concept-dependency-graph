package com.mathdep.controller;

import com.mathdep.service.ai.AiExtractionException;
import com.mathdep.service.document.DocumentTextExtractionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AiExtractionException.class)
    public ResponseEntity<Map<String, Object>> handleAiExtraction(AiExtractionException ex) {
        log.warn("AI extraction request failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
            "error", "AI_EXTRACTION_FAILED",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(DocumentTextExtractionException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentExtraction(DocumentTextExtractionException ex) {
        log.warn("Document extraction request failed: {}", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
            "error", "DOCUMENT_TEXT_EXTRACTION_FAILED",
            "message", ex.getMessage()
        ));
    }
}
