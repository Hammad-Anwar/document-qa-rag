package com.example.documentqa.dto;

public record SourceResponse(int chunkIndex, Integer page, String content, double similarity) {}
