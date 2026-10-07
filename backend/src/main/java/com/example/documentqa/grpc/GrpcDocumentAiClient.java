package com.example.documentqa.grpc;

import com.example.documentqa.dto.AiAnswer;
import com.example.documentqa.dto.SourceResponse;
import com.example.documentqa.exception.ApiException;
import com.example.documentqa.proto.*;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcDocumentAiClient implements DocumentAiClient {
    private final DocumentAIServiceGrpc.DocumentAIServiceBlockingStub stub;
    private final long deadline;

    public GrpcDocumentAiClient(DocumentAIServiceGrpc.DocumentAIServiceBlockingStub stub,
        @Value("${ai.grpc.deadline-seconds:300}") long deadline) {
        this.stub = stub;
        this.deadline = deadline;
    }

    public int process(UUID id, String filename, byte[] content) {
        try {
            return stub.withDeadlineAfter(deadline, TimeUnit.SECONDS).processDocument(
                ProcessDocumentRequest.newBuilder().setDocumentId(id.toString())
                    .setFilename(filename).setFileContent(ByteString.copyFrom(content)).build())
                .getChunkCount();
        } catch (StatusRuntimeException exception) { throw mapped(exception); }
    }

    public AiAnswer ask(UUID id, String question) {
        try {
            var result = stub.withDeadlineAfter(deadline, TimeUnit.SECONDS).askQuestion(
                com.example.documentqa.proto.QuestionRequest.newBuilder()
                    .setDocumentId(id.toString()).setQuestion(question).build());
            return new AiAnswer(result.getAnswer(), result.getSourcesList().stream().map(source ->
                new SourceResponse(source.getChunkIndex(), source.hasPage() ? source.getPage() : null,
                    source.getContent(), source.getSimilarity())).toList());
        } catch (StatusRuntimeException exception) { throw mapped(exception); }
    }

    private ApiException mapped(StatusRuntimeException exception) {
        return switch (exception.getStatus().getCode()) {
            case INVALID_ARGUMENT -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "DOCUMENT_PROCESSING_ERROR", exception.getStatus().getDescription() == null
                    ? "Document or question could not be processed" : exception.getStatus().getDescription());
            case DEADLINE_EXCEEDED -> new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                "AI_TIMEOUT", "AI processing exceeded the configured deadline");
            default -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI_SERVICE_UNAVAILABLE", "AI service or its model/database dependency is unavailable");
        };
    }
}
