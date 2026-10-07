package com.example.documentqa.dto;

import java.util.List;

public record AiAnswer(String answer, List<SourceResponse> sources) {}
