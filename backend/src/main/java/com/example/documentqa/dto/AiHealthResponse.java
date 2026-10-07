package com.example.documentqa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI service health status")
public record AiHealthResponse(
    @Schema(description = "Health status", example = "UP")
    String status,
    @Schema(description = "Greeting from the Python AI service", example = "Hello Spring Boot from Python AI service")
    String message) {}
