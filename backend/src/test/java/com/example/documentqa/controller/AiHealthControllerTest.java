package com.example.documentqa.controller;

import com.example.documentqa.exception.AiServiceUnavailableException;
import com.example.documentqa.grpc.AiServiceClient;
import com.example.documentqa.service.AiHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiHealthControllerTest {

    @Test
    void returnsPythonPingAsJson() throws Exception {
        MockMvc mockMvc = mockMvcFor(
            name -> "Hello Spring Boot from Python AI service"
        );

        mockMvc.perform(get("/api/health/ai"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.message").value("Hello Spring Boot from Python AI service"));
    }

    @Test
    void returns503WhenPythonIsUnavailable() throws Exception {
        MockMvc mockMvc = mockMvcFor(name -> {
            throw new AiServiceUnavailableException("Python AI service is unavailable");
        });

        mockMvc.perform(get("/api/health/ai"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("AI_SERVICE_UNAVAILABLE"))
            .andExpect(jsonPath("$.message").value("Python AI service is unavailable"));
    }

    private MockMvc mockMvcFor(AiServiceClient aiServiceClient) {
        AiHealthService aiHealthService = new AiHealthService(aiServiceClient);
        AiHealthController controller = new AiHealthController(aiHealthService);

        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new com.example.documentqa.exception.GlobalExceptionHandler())
            .build();
    }
}
