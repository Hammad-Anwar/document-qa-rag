package com.example.documentqa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Question to ask about a document")
public record QuestionRequest(
    @Schema(description = "The question text (1–2000 characters)",
        example = "How does TCP provide reliable delivery?", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 2000) String question) {}
