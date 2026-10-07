# Document QA RAG

This repository is the foundation for a document question-answering backend. The current milestone intentionally implements only a small end-to-end check:

```text
HTTP client -> Spring Boot -> gRPC -> Python AI service
```

Calling Spring Boot returns a greeting created by Python. This proves that the Java and Python services can build, start, find each other on the Docker network, and communicate through their shared Protocol Buffer contract.

PostgreSQL, pgvector, LangChain, Ollama, document upload, and RAG are folder-level scaffolding only. They are not installed or started yet.

## Prerequisites

- Docker Engine or Docker Desktop
- Docker Compose v2 (`docker compose`)
- `curl` for the example request

Java, Maven, and Python do not need to be installed locally when you use Docker.

## Run with Docker

From the `document-qa-rag` repository directory:

```bash
cp .env.example .env
docker compose up --build -d
docker compose ps
```

The first build downloads the Java, Maven, and Python base images plus project dependencies, so it takes longer than later builds.

Check the complete flow:

```bash
curl http://localhost:8080/api/health/ai
```

Expected response:

```json
{"status":"UP","message":"Hello Spring Boot from Python AI service"}
```

Follow the logs:

```bash
docker compose logs -f backend ai-service
```

Stop and remove the containers and project network:

```bash
docker compose down
```

Rebuild after changing source or the protobuf contract:

```bash
docker compose up --build -d
```

## What Docker starts

| Service | Role | Port |
| --- | --- | --- |
| `backend` | Spring Boot REST API and gRPC client | Host `8080` -> container `8080` |
| `ai-service` | Python gRPC server | Internal Docker port `50051` |

The Python port is not published to the host. Spring Boot reaches it using the Docker service name `ai-service`.

## Request lifecycle

1. The client sends `GET /api/health/ai` to Spring Boot.
2. `AiHealthController` calls `AiHealthService`.
3. `GrpcAiServiceClient` sends `PingRequest(name = "Spring Boot")` to `ai-service:50051` with a three-second deadline.
4. Python's `DocumentAIService` returns the greeting.
5. Spring Boot returns the greeting as JSON.

If Python cannot be reached, Spring Boot returns HTTP `503 Service Unavailable` instead of waiting indefinitely.

## Repository structure

```text
document-qa-rag/
├── backend/                 Spring Boot service
├── ai-service/              Python gRPC service and future AI packages
├── proto/                   Shared Java/Python gRPC contract
├── database/                Future PostgreSQL + pgvector assets
├── docker/                  Future shared Docker helpers
├── docs/                    Architecture, API, design, and plans
├── docker-compose.yml       Two-service local runtime
├── .env.example             Safe local configuration example
└── README.md                This guide
```

Inside `backend`, the normal controller, service, gRPC, DTO, configuration, exception, entity, repository, and migration areas are present. Inside `ai-service/app`, the database, document, embeddings, retrieval, and RAG areas explain what belongs there in later milestones.

## Shared protobuf generation

The source contract is [`proto/document_ai.proto`](proto/document_ai.proto). Do not edit generated files.

- Maven generates Java sources automatically during the backend build.
- The Python Dockerfile generates Python sources while building the image.
- Generated sources and caches are excluded by `.gitignore` and `.dockerignore`.

## Run tests

### Python

If Python 3.12 is installed locally:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r ai-service/requirements.txt
python -m grpc_tools.protoc \
  -Iproto \
  --python_out=ai-service/app/generated \
  --grpc_python_out=ai-service/app/generated \
  proto/document_ai.proto
python -m pytest ai-service/tests -v
```

### Spring Boot

With Maven and Java 21 installed:

```bash
mvn -f backend/pom.xml test
```

Without local Maven, use Docker:

```bash
docker run --rm \
  -v "$PWD:/workspace" \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -f backend/pom.xml test
```

## Configuration

`.env.example` documents the available local values:

| Variable | Default | Meaning |
| --- | --- | --- |
| `BACKEND_PORT` | `8080` | Port exposed on the host |
| `AI_SERVICE_PORT` | `50051` | Python gRPC port inside Docker |

For a native Spring run, `AI_SERVICE_HOST` defaults to `localhost`. Docker Compose overrides it to `ai-service`.

## Troubleshooting

Check service state and recent logs:

```bash
docker compose ps
docker compose logs --tail=100 backend ai-service
```

If port `8080` is already used, change `BACKEND_PORT` in `.env`, rebuild, and call the new port.

If the backend returns `503`, confirm that `ai-service` is healthy. Compose waits for the Python gRPC port before starting the backend, but a later Python crash will still be reported as `503`.

## Future learning milestones

The attached project brief can be implemented incrementally after this foundation:

1. Add document metadata and upload endpoints in Spring Boot.
2. Send PDF/TXT bytes to Python over a new RPC.
3. Extract and split text.
4. Add PostgreSQL and pgvector.
5. Generate embeddings and verify similarity retrieval.
6. Add LangChain and Ollama for grounded answers.
7. Store question and answer history.

Keeping these stages separate makes each technology boundary testable before adding the next one.
