package com.example.documentqa.dto;

import java.util.UUID;

public record DocumentErrorResponse(String error, String message, UUID documentId) {}
