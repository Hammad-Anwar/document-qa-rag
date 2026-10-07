package com.example.documentqa.config;

import com.example.documentqa.proto.DocumentAIServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Bean(destroyMethod = "shutdownNow")
    ManagedChannel aiServiceChannel(
        @Value("${ai.grpc.host}") String host,
        @Value("${ai.grpc.port}") int port
    ) {
        return ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
    }

    @Bean
    DocumentAIServiceGrpc.DocumentAIServiceBlockingStub aiServiceStub(
        ManagedChannel aiServiceChannel
    ) {
        return DocumentAIServiceGrpc.newBlockingStub(aiServiceChannel);
    }
}
