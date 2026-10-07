package com.example.documentqa.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record QuestionResponse(UUID id, UUID documentId, String question, String answer,
    List<SourceResponse> sources, OffsetDateTime createdAt) {}
