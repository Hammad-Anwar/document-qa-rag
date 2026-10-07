package com.example.documentqa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Retrieved source excerpt with location and relevance score")
public record SourceResponse(
    @Schema(description = "Zero-based chunk index within the document", example = "0")
    int chunkIndex,
    @Schema(description = "One-based PDF page number (null for TXT)", example = "1")
    Integer page,
    @Schema(description = "Retrieved text excerpt", example = "TCP provides reliable delivery using acknowledgements.")
    String content,
    @Schema(description = "Cosine similarity score (0–1)", example = "0.85")
    double similarity) {}
