# gRPC Ping Foundation Design

## Goal

Create the complete folder scaffold for the future Document QA RAG monorepo while implementing only one executable vertical slice: Spring Boot calls a Python AI service through gRPC and exposes the result over HTTP. Both services must run with Docker Compose.

## Scope

The repository will contain top-level areas for the future Spring backend, Python AI service, shared protobuf contract, database assets, Docker assets, and documentation. Empty future areas will contain short README files or package placeholders that state their intended responsibility.

This milestone will not implement document upload, PDF parsing, text splitting, embeddings, vector search, PostgreSQL access, LangChain, Ollama, question history, authentication, or the RAG pipeline. PostgreSQL, pgvector, and Ollama will not be started by Docker Compose yet.

## Architecture

Docker Compose will run two containers on one internal network:

1. `ai-service` runs a Python gRPC server on port `50051`.
2. `backend` runs Spring Boot on port `8080` and connects to `ai-service:50051`.

The shared contract in `proto/document_ai.proto` will define a `DocumentAIService` with a unary `Ping` RPC. Python returns a small greeting. Spring Boot exposes `GET /api/health/ai`, calls `Ping`, and translates the result into a JSON HTTP response.

Generated protobuf source files are build artifacts. Maven generates Java classes during the backend build, and the Python container generates Python classes during its image build. Generated files will not be maintained by hand.

## Repository Layout

```text
document-qa-rag/
├── backend/
│   ├── src/main/java/com/example/documentqa/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── grpc/
│   │   ├── repository/
│   │   └── service/
│   ├── src/main/resources/db/migration/
│   ├── src/test/
│   ├── Dockerfile
│   └── pom.xml
├── ai-service/
│   ├── app/
│   │   ├── database/
│   │   ├── document/
│   │   ├── embeddings/
│   │   ├── generated/
│   │   ├── rag/
│   │   └── retrieval/
│   ├── tests/
│   ├── Dockerfile
│   └── requirements.txt
├── proto/document_ai.proto
├── database/
├── docker/
├── docs/
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

The outer `document-backend/` directory will contain a concise guide pointing to the nested repository and showing the exact Docker commands. All application source and configuration remain inside `document-qa-rag/`.

## Request Flow

1. A caller sends `GET /api/health/ai` to Spring Boot.
2. Spring Boot creates a gRPC `PingRequest` and calls the configured AI service address.
3. Python handles the request and returns `Hello from Python AI service`.
4. Spring Boot returns a JSON response containing the message and a healthy status.

If Python is unavailable or the RPC deadline expires, Spring Boot returns HTTP `503 Service Unavailable` with a stable error payload. The gRPC call will use a short deadline so an unhealthy dependency cannot leave the HTTP request hanging.

## Configuration

Spring Boot reads the AI service host and port from environment variables with local defaults. The Python service reads its gRPC port from an environment variable. `.env.example` documents these values and contains no secrets.

## Testing and Verification

- A Python unit test calls the ping service method directly and verifies its reply.
- A Spring test verifies the HTTP controller using a mocked gRPC-facing service boundary.
- Maven and pytest run independently outside Docker when their runtimes are available.
- The final integration check builds both images with Docker Compose, starts the services, calls `http://localhost:8080/api/health/ai`, and verifies the greeting returned by Python.

## Documentation

The repository README will explain the architecture, prerequisites, folder purposes, native test commands, Docker commands, expected response, logs, shutdown, and the intentionally deferred RAG stages. The outer-folder guide will contain only navigation and run instructions so it does not duplicate the main documentation.
