package com.example.documentqa.grpc;

import com.example.documentqa.dto.AiAnswer;
import java.util.UUID;

public interface DocumentAiClient {
    int process(UUID documentId, String filename, byte[] content);
    AiAnswer ask(UUID documentId, String question);
}
