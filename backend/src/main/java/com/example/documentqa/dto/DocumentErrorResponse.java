package com.example.documentqa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Document processing error with document ID for inspection")
public record DocumentErrorResponse(
    @Schema(description = "Machine-readable error code", example = "DOCUMENT_PROCESSING_ERROR")
    String error,
    @Schema(description = "Human-readable error description", example = "PDF could not be read")
    String message,
    @Schema(description = "ID of the FAILED document record", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID documentId) {}
