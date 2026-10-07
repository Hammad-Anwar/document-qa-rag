package com.example.documentqa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Document metadata and processing status")
public record DocumentResponse(
    @Schema(description = "Unique document identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    @Schema(description = "Original filename", example = "networking.txt")
    String filename,
    @Schema(description = "File type", example = "TXT", allowableValues = {"PDF", "TXT"})
    String fileType,
    @Schema(description = "File size in bytes", example = "1024")
    long fileSize,
    @Schema(description = "Processing status", example = "READY", allowableValues = {"PROCESSING", "READY", "FAILED"})
    String status,
    @Schema(description = "Number of stored chunks", example = "3")
    int chunkCount,
    @Schema(description = "Failure message (null when READY)", example = "null")
    String errorMessage,
    @Schema(description = "Creation timestamp", example = "2026-01-01T12:00:00Z")
    OffsetDateTime createdAt) {}
