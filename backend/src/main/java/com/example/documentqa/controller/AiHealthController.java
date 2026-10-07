package com.example.documentqa.controller;

import com.example.documentqa.dto.AiHealthResponse;
import com.example.documentqa.dto.ApiErrorResponse;
import com.example.documentqa.service.AiHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health/ai")
@Tag(name = "Health", description = "AI service connectivity checks")
public class AiHealthController {

    private final AiHealthService aiHealthService;

    public AiHealthController(AiHealthService aiHealthService) {
        this.aiHealthService = aiHealthService;
    }

    @Operation(summary = "Check AI service health",
        description = "Tests gRPC connectivity from Spring Boot to the Python AI service. "
            + "Does not independently validate model quality or database health.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "AI service is reachable",
            content = @Content(schema = @Schema(implementation = AiHealthResponse.class))),
        @ApiResponse(responseCode = "503", description = "AI service is unavailable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    })
    @GetMapping
    public AiHealthResponse health() {
        return aiHealthService.check();
    }
}
