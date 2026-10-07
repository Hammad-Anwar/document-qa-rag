package com.example.documentqa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Question with answer, retrieved sources, and timestamp")
public record QuestionResponse(
    @Schema(description = "Unique question identifier", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    UUID id,
    @Schema(description = "Parent document identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID documentId,
    @Schema(description = "The question that was asked", example = "How does TCP provide reliable delivery?")
    String question,
    @Schema(description = "The generated answer", example = "TCP uses acknowledgements and retransmissions. [1]")
    String answer,
    @Schema(description = "Retrieved source excerpts used as context")
    List<SourceResponse> sources,
    @Schema(description = "Creation timestamp", example = "2026-01-01T12:00:00Z")
    OffsetDateTime createdAt) {}
