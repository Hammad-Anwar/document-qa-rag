package com.example.documentqa.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(UUID id, String filename, String fileType, long fileSize,
    String status, int chunkCount, String errorMessage, OffsetDateTime createdAt) {}
