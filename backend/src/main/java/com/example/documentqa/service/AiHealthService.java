package com.example.documentqa.service;

import com.example.documentqa.dto.AiHealthResponse;
import com.example.documentqa.grpc.AiServiceClient;
import org.springframework.stereotype.Service;

@Service
public class AiHealthService {

    private final AiServiceClient aiServiceClient;

    public AiHealthService(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    public AiHealthResponse check() {
        return new AiHealthResponse("UP", aiServiceClient.ping("Spring Boot"));
    }
}
