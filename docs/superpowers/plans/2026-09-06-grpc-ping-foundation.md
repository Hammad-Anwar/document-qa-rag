# gRPC Ping Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Dockerized Spring Boot-to-Python gRPC ping path inside a complete Document QA RAG monorepo scaffold.

**Architecture:** A shared protobuf contract defines `DocumentAIService/Ping`. The Python container serves the RPC, while Spring Boot wraps a blocking gRPC stub behind a small client boundary and exposes it through `GET /api/health/ai`; future RAG packages remain non-executable scaffolding.

**Tech Stack:** Java 21, Spring Boot, Maven, grpc-java, Protocol Buffers, Python 3.12, grpcio, pytest, Docker, Docker Compose

**Spec:** `docs/superpowers/specs/2026-09-06-grpc-ping-foundation-design.md`

## Global Constraints

- All application source and configuration stays inside `document-qa-rag/`.
- The outer `document-backend/` directory receives only a concise navigation and run guide.
- Docker Compose starts only `backend` and `ai-service` in this milestone.
- The only behavior is `DocumentAIService/Ping` plus `GET /api/health/ai`.
- A failed or timed-out Python call produces HTTP 503 with a stable JSON error body.
- Generated protobuf code is produced during builds and is not maintained by hand.
- PostgreSQL, pgvector, LangChain, Ollama, upload, parsing, embeddings, retrieval, and RAG remain unimplemented.

---

### Task 1: Shared gRPC Contract and Python Ping Service

**Files:**
- Create: `proto/document_ai.proto`
- Create: `ai-service/app/__init__.py`
- Create: `ai-service/app/grpc_server.py`
- Create: `ai-service/app/main.py`
- Create: `ai-service/tests/test_grpc_server.py`
- Create: `ai-service/requirements.txt`
- Create: `ai-service/pytest.ini`

**Interfaces:**
- Produces: protobuf service `documentqa.v1.DocumentAIService` with `rpc Ping(PingRequest) returns (PingResponse)`
- Produces: `DocumentAIService.Ping(request, context) -> PingResponse`
- Produces: `serve(port: int) -> None`

- [ ] **Step 1: Define the protobuf contract**

```proto
syntax = "proto3";
package documentqa.v1;
option java_multiple_files = true;
option java_package = "com.example.documentqa.proto";

service DocumentAIService {
  rpc Ping(PingRequest) returns (PingResponse);
}

message PingRequest { string name = 1; }
message PingResponse { string message = 1; }
```

- [ ] **Step 2: Write the failing Python service test**

```python
def test_ping_returns_ai_service_greeting():
    response = DocumentAIService().Ping(PingRequest(name="Spring Boot"), None)
    assert response.message == "Hello Spring Boot from Python AI service"
```

- [ ] **Step 3: Run the test and verify the service is missing**

Run: `python -m pytest ai-service/tests/test_grpc_server.py -v`
Expected: FAIL because the generated protobuf modules or service implementation do not exist.

- [ ] **Step 4: Add the minimal Python service and server entrypoint**

```python
class DocumentAIService(document_ai_pb2_grpc.DocumentAIServiceServicer):
    def Ping(self, request, context):
        name = request.name.strip() or "Spring Boot"
        return document_ai_pb2.PingResponse(
            message=f"Hello {name} from Python AI service"
        )
```

`serve()` will register this servicer on a thread-pool gRPC server, bind `[::]:<port>`, start, and wait for termination. `main.py` reads `GRPC_PORT`, defaulting to `50051`.

- [ ] **Step 5: Generate Python protobuf files and run the test**

Run:

```bash
python -m grpc_tools.protoc -Iproto --python_out=ai-service/app/generated --grpc_python_out=ai-service/app/generated proto/document_ai.proto
python -m pytest ai-service/tests/test_grpc_server.py -v
```

Expected: PASS with one test.

- [ ] **Step 6: Commit the Python vertical slice**

```bash
git add proto ai-service
git commit -m "feat: add Python gRPC ping service"
```

### Task 2: Spring Boot gRPC Client and HTTP Health Endpoint

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/example/documentqa/DocumentQaApplication.java`
- Create: `backend/src/main/java/com/example/documentqa/config/GrpcClientConfig.java`
- Create: `backend/src/main/java/com/example/documentqa/grpc/AiServiceClient.java`
- Create: `backend/src/main/java/com/example/documentqa/grpc/GrpcAiServiceClient.java`
- Create: `backend/src/main/java/com/example/documentqa/service/AiHealthService.java`
- Create: `backend/src/main/java/com/example/documentqa/controller/AiHealthController.java`
- Create: `backend/src/main/java/com/example/documentqa/dto/AiHealthResponse.java`
- Create: `backend/src/main/java/com/example/documentqa/dto/ApiErrorResponse.java`
- Create: `backend/src/main/java/com/example/documentqa/exception/AiServiceUnavailableException.java`
- Create: `backend/src/main/java/com/example/documentqa/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/example/documentqa/controller/AiHealthControllerTest.java`

**Interfaces:**
- Consumes: generated `DocumentAIServiceGrpc`, `PingRequest`, and `PingResponse`
- Produces: `AiServiceClient.ping(String name) -> String`
- Produces: `AiHealthService.check() -> AiHealthResponse`
- Produces: `GET /api/health/ai` returning `{"status":"UP","message":"Hello Spring Boot from Python AI service"}`

- [ ] **Step 1: Create the Maven build with protobuf generation**

Configure Java 21, Spring Boot web/test, grpc-netty-shaded, grpc-protobuf, grpc-stub, protobuf-java, `protobuf-maven-plugin`, and `os-maven-plugin`. Generate Java sources from `../proto/document_ai.proto` during `generate-sources`.

- [ ] **Step 2: Write the failing controller tests**

```java
@WebMvcTest(AiHealthController.class)
class AiHealthControllerTest {
    @MockBean AiHealthService aiHealthService;

    @Test
    void returnsPythonPingAsJson() throws Exception {
        given(aiHealthService.check()).willReturn(
            new AiHealthResponse("UP", "Hello Spring Boot from Python AI service"));
        mockMvc.perform(get("/api/health/ai"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void returns503WhenPythonIsUnavailable() throws Exception {
        given(aiHealthService.check()).willThrow(
            new AiServiceUnavailableException("Python AI service is unavailable"));
        mockMvc.perform(get("/api/health/ai"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("AI_SERVICE_UNAVAILABLE"));
    }
}
```

- [ ] **Step 3: Run the tests and verify the application types are missing**

Run: `mvn -f backend/pom.xml test`
Expected: FAIL during test compilation because the controller, DTOs, and service are missing.

- [ ] **Step 4: Implement the HTTP and service boundaries**

```java
public interface AiServiceClient {
    String ping(String name);
}

@Service
public class AiHealthService {
    public AiHealthResponse check() {
        return new AiHealthResponse("UP", aiServiceClient.ping("Spring Boot"));
    }
}

@RestController
@RequestMapping("/api/health/ai")
public class AiHealthController {
    @GetMapping
    public AiHealthResponse health() { return aiHealthService.check(); }
}
```

The production client uses a blocking stub with a three-second deadline. It maps `StatusRuntimeException` to `AiServiceUnavailableException`. `GrpcClientConfig` creates a plaintext managed channel from `${AI_SERVICE_HOST:localhost}` and `${AI_SERVICE_PORT:50051}` and closes it during shutdown.

- [ ] **Step 5: Run the Spring tests**

Run: `mvn -f backend/pom.xml test`
Expected: PASS with both success and dependency-failure controller tests.

- [ ] **Step 6: Commit the Spring vertical slice**

```bash
git add backend
git commit -m "feat: expose Python gRPC ping over HTTP"
```

### Task 3: Complete Future Project Scaffold and Docker Runtime

**Files:**
- Create: `ai-service/app/database/README.md`
- Create: `ai-service/app/document/README.md`
- Create: `ai-service/app/embeddings/README.md`
- Create: `ai-service/app/rag/README.md`
- Create: `ai-service/app/retrieval/README.md`
- Create: `backend/src/main/java/com/example/documentqa/entity/package-info.java`
- Create: `backend/src/main/java/com/example/documentqa/repository/package-info.java`
- Create: `backend/src/main/resources/db/migration/README.md`
- Create: `database/README.md`
- Create: `docker/README.md`
- Create: `backend/Dockerfile`
- Create: `ai-service/Dockerfile`
- Create: `docker-compose.yml`
- Create: `.dockerignore`
- Create: `.env.example`
- Create: `.gitignore`

**Interfaces:**
- Consumes: backend build from Task 2 and Python entrypoint from Task 1
- Produces: Compose services `ai-service` and `backend`
- Produces: public HTTP port `8080`; internal gRPC port `50051`

- [ ] **Step 1: Add focused scaffold markers**

Each future Python package README will name the deferred responsibility and explicitly state that it is not implemented. Java package descriptors will document future persistence responsibilities without defining runtime classes.

- [ ] **Step 2: Add a multi-stage Spring Dockerfile**

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY proto /workspace/proto
COPY backend /workspace/backend
RUN mvn -f backend/pom.xml clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 3: Add the Python Dockerfile**

```dockerfile
FROM python:3.12-slim
WORKDIR /workspace
COPY ai-service/requirements.txt ./ai-service/requirements.txt
RUN pip install --no-cache-dir -r ai-service/requirements.txt
COPY proto ./proto
COPY ai-service ./ai-service
RUN mkdir -p ai-service/app/generated && \
    python -m grpc_tools.protoc -Iproto \
      --python_out=ai-service/app/generated \
      --grpc_python_out=ai-service/app/generated \
      proto/document_ai.proto
ENV PYTHONPATH=/workspace/ai-service
EXPOSE 50051
CMD ["python", "-m", "app.main"]
```

- [ ] **Step 4: Add Docker Compose health-aware startup**

Define `ai-service` with a gRPC health check or a Python socket check, then make `backend` depend on its healthy state. Set `AI_SERVICE_HOST=ai-service`, `AI_SERVICE_PORT=50051`, and expose only backend port `8080` to the host.

- [ ] **Step 5: Validate Compose configuration**

Run: `docker compose config`
Expected: exit code 0 with exactly two services, `backend` and `ai-service`.

- [ ] **Step 6: Build and start the application**

Run: `docker compose up --build -d`
Expected: both containers become healthy/running.

- [ ] **Step 7: Verify the complete cross-language flow**

Run: `curl --fail --silent http://localhost:8080/api/health/ai`
Expected:

```json
{"status":"UP","message":"Hello Spring Boot from Python AI service"}
```

- [ ] **Step 8: Commit Docker and scaffold files**

```bash
git add .
git commit -m "build: add Dockerized project scaffold"
```

### Task 4: Learning and Run Documentation

**Files:**
- Create: `README.md`
- Create: `docs/architecture.md`
- Create: `docs/api.md`
- Create: `../README.md`

**Interfaces:**
- Consumes: verified commands and response from Tasks 1-3
- Produces: exact beginner-friendly setup, run, test, inspect, and stop instructions

- [ ] **Step 1: Document the repository**

Explain prerequisites, architecture, every top-level folder, protobuf generation, Docker startup, expected response, logs, shutdown, rebuild, native tests, and troubleshooting. Clearly separate the working ping milestone from the future RAG roadmap.

- [ ] **Step 2: Document the API and request path**

Record `GET /api/health/ai`, its 200 response, its 503 response, and the HTTP → Spring service → gRPC client → Python server flow.

- [ ] **Step 3: Add the outer-folder guide**

The outer `document-backend/README.md` will direct the learner to run:

```bash
cd document-qa-rag
cp .env.example .env
docker compose up --build
```

It will link to the repository README for deeper explanation.

- [ ] **Step 4: Re-run all verification**

Run:

```bash
python -m pytest ai-service/tests -v
mvn -f backend/pom.xml test
docker compose config
curl --fail --silent http://localhost:8080/api/health/ai
```

Expected: all tests pass, Compose validates, and curl returns the Python greeting.

- [ ] **Step 5: Commit documentation**

```bash
git add README.md docs ../README.md
git commit -m "docs: explain gRPC ping project workflow"
```
