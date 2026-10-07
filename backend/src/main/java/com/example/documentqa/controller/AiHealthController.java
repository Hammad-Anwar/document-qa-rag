package com.example.documentqa.controller;

import com.example.documentqa.dto.AiHealthResponse;
import com.example.documentqa.service.AiHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health/ai")
public class AiHealthController {

    private final AiHealthService aiHealthService;

    public AiHealthController(AiHealthService aiHealthService) {
        this.aiHealthService = aiHealthService;
    }

    @GetMapping
    public AiHealthResponse health() {
        return aiHealthService.check();
    }
}
