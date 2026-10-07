package com.example.documentqa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API error response")
public record ApiErrorResponse(
    @Schema(description = "Machine-readable error code", example = "INVALID_REQUEST")
    String error,
    @Schema(description = "Human-readable error description", example = "Check the document ID, file part, and question")
    String message) {}
