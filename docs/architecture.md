# Architecture

## Working milestone

```text
curl / browser
      |
      | HTTP GET :8080/api/health/ai
      v
Spring Boot backend
      |
      | gRPC Ping, three-second deadline
      v
Python AI service :50051
```

The Spring Boot application remains the main backend and owns the public HTTP boundary. Python is a private AI-oriented service. The shared contract in `proto/document_ai.proto` is the only coupling between their language-specific implementations.

## Component responsibilities

### Spring Boot

- Accept the public health request.
- Call the Python service through a narrow `AiServiceClient` interface.
- Convert gRPC availability failures into a stable HTTP 503 response.
- Read the Python host and port from configuration.

### Python

- Listen for gRPC requests.
- Implement the `Ping` RPC.
- Return the greeting used to prove cross-language communication.

### Protocol Buffers

- Define service names, RPCs, and messages once.
- Generate Java classes during Maven builds.
- Generate Python modules during Python image builds.

## Future architecture

Later milestones will keep Spring Boot as the application backend and extend Python with document loading, splitting, embeddings, retrieval, and grounded answer generation. PostgreSQL will hold relational application data and pgvector embeddings. Ollama will remain behind a replaceable LangChain model boundary.

Those services are deliberately absent from the current Compose file so the ping milestone stays fast and understandable.
