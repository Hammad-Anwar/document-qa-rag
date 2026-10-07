package com.example.documentqa.grpc;

import com.example.documentqa.exception.AiServiceUnavailableException;
import com.example.documentqa.proto.DocumentAIServiceGrpc;
import com.example.documentqa.proto.PingRequest;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class GrpcAiServiceClient implements AiServiceClient {

    private static final long DEADLINE_SECONDS = 3;

    private final DocumentAIServiceGrpc.DocumentAIServiceBlockingStub aiServiceStub;

    public GrpcAiServiceClient(
        DocumentAIServiceGrpc.DocumentAIServiceBlockingStub aiServiceStub
    ) {
        this.aiServiceStub = aiServiceStub;
    }

    @Override
    public String ping(String name) {
        try {
            return aiServiceStub
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .ping(PingRequest.newBuilder().setName(name).build())
                .getMessage();
        } catch (StatusRuntimeException exception) {
            throw new AiServiceUnavailableException(
                "Python AI service is unavailable",
                exception
            );
        }
    }
}
