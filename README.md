# Document QA RAG

Upload a PDF or UTF-8 TXT file, ask questions about its contents, and inspect answers with retrieved sources and saved history.

Spring Boot owns the REST API, document status, and question history. Python uses pypdf, LangChain, Ollama embeddings, and PostgreSQL/pgvector to process and retrieve document chunks. Both services communicate through gRPC.

## Start

Install Docker Engine/Desktop with Docker Compose. Local Java, Maven, and Python installations are optional.

From the outer document-backend folder:

```bash
cd document-qa-rag
# Optional: copy only on first setup; preserve an existing .env.
cp -n .env.example .env
```

Set a unique POSTGRES_PASSWORD in your ignored .env before starting. You can generate a value locally with `openssl rand -hex 32`; do not commit or share it. The example intentionally leaves the password blank, and Compose rejects missing/empty passwords. If you already have a database volume, follow the rotation steps below before changing its password.

Then start:

```bash
docker compose up --build -d
docker compose ps -a
docker compose logs -f ollama-init backend ai-service
```

First startup downloads container images and two models: llama3.2:3b for answers and nomic-embed-text for embeddings. This needs several GB of disk space and internet access. Allow roughly 4 GB of free RAM for model execution, plus the backend/database. CPU execution works; allow several minutes on first use. PostgreSQL migrations run automatically when Spring starts. ollama-init should finish with exit code 0.

The API is available at http://localhost:8080. Database, gRPC, and Ollama ports stay inside Docker.

**Swagger UI**: interactive API docs at http://localhost:8080/swagger-ui.html. The OpenAPI JSON is at http://localhost:8080/v3/api-docs.

## Upload and ask

```bash
curl -sS -F "file=@examples/networking.txt" http://localhost:8080/api/documents
```

Copy the returned id, then use it below:

```bash
DOCUMENT_ID="paste-the-returned-id"
curl -sS "http://localhost:8080/api/documents/$DOCUMENT_ID"
curl -sS -X POST "http://localhost:8080/api/documents/$DOCUMENT_ID/questions" \
  -H "Content-Type: application/json" \
  -d '{"question":"How do TCP and UDP differ?"}'
curl -sS "http://localhost:8080/api/documents/$DOCUMENT_ID/questions"
```

Upload responds with HTTP 201 and READY status after processing completes. A question responds with HTTP 201, an answer, source excerpts, chunk indices, PDF pages (null for TXT), similarity scores, and a saved question ID.

Postman: import [the collection](examples/document-qa.postman_collection.json), select examples/networking.txt in the upload request, and run the requests in order. Upload saves documentId automatically. Change the question for your own document.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| POST | /api/documents | Upload PDF/TXT, max 10 MiB |
| GET | /api/documents/{id} | Metadata and processing status |
| POST | /api/documents/{id}/questions | Ask a 1–2000 character question |
| GET | /api/documents/{id}/questions?limit=50&offset=0 | Saved history; max page size 100 |
| GET | /api/health/ai | Check Spring -> Python gRPC connectivity |

Swagger UI: http://localhost:8080/swagger-ui.html · OpenAPI JSON: http://localhost:8080/v3/api-docs

See [API details](docs/api.md).

## Flow

```text
Upload -> Spring validation -> committed PROCESSING record
       -> Python gRPC -> extract text -> split into chunks
       -> Ollama embeddings -> atomic pgvector insert -> READY

Question -> Spring READY check -> Python gRPC -> question embedding
         -> pgvector search filtered by document ID -> LangChain prompt
         -> Ollama answer -> sources -> saved question/answer -> REST response
```

Original files are processed in memory and not permanently stored. PDF page numbers are one-based, chunk indices zero-based. Scanned and encrypted PDFs are rejected. Text extraction is capped at 500 PDF pages, 2 million characters, and 3000 chunks.

An upload failure preserves a FAILED metadata record; its error response includes documentId so you can inspect it. A later timeout or model outage returns a clear error rather than an invented answer.

## Folders

- backend: Java controllers, services, JDBC persistence, gRPC clients, Flyway migrations, tests, Dockerfile.
- ai-service: Python document loader/splitter/processor, embeddings, vector storage, RAG prompt/chain, gRPC server, tests, Dockerfile.
- proto: shared source contract; both languages generate their bindings during builds.
- database: schema ownership and database notes.
- docker: runtime notes.
- docs: architecture, API, configuration, and implementation plan.
- examples: sample upload.
- scripts: full smoke test against the running services.

## Run tests

Python unit tests:

```bash
docker compose exec -T -w /workspace/ai-service ai-service python -m pytest tests -q
```

Python direct dependencies are listed in requirements.txt; the Docker build installs the fully pinned requirements.lock. Regenerate the lock in a clean Python 3.12 environment after changing direct dependencies, then rebuild and rerun both suites.

Tests that need a live pgvector database skip until TEST_DATABASE_URL is set. To run them with Compose's configured database:

```bash
docker compose exec -T -w /workspace/ai-service ai-service \
  sh -c 'TEST_DATABASE_URL="$DATABASE_URL" python -m pytest tests -q'
```

Spring unit/API tests (Java 21 and Maven are supplied by the image):

```bash
docker run --rm -v "$PWD:/workspace" -w /workspace \
  -v documentqa-maven-cache:/root/.m2 \
  maven:3.9-eclipse-temurin-21 mvn -B -ntp -f backend/pom.xml test
```

Real end-to-end smoke test:

```bash
docker compose exec -T ai-service python /workspace/scripts/smoke_test.py
```

The smoke test creates demo documents and question history and leaves them available for inspection. Live vector-store tests create unique temporary records and delete only their own records afterward.

## Configuration

Edit .env, then run docker compose up --build -d. See [.env.example](.env.example) for all defaults.

| Variable | Default | Meaning |
| --- | --- | --- |
| BACKEND_PORT | 8080 | Local REST port |
| POSTGRES_DB / POSTGRES_USER | documentqa | Local database and role names |
| POSTGRES_PASSWORD | Required; no default | Set only in ignored .env or runtime environment |
| LLM_MODEL | llama3.2:3b | Local chat model; tested on CPU |
| EMBEDDING_MODEL | nomic-embed-text | Embedding model |
| EMBEDDING_DIMENSIONS | 768 | Required embedding length |
| RETRIEVAL_TOP_K | 4 | Maximum returned chunks |
| RETRIEVAL_MIN_SIMILARITY | 0.35 | Minimum cosine similarity |
| AI_RPC_DEADLINE_SECONDS | 300 | Maximum document/question RPC duration |
| MODEL_TIMEOUT_SECONDS | 240 | Individual Ollama request timeout |
| MODEL_CONTEXT_TOKENS | 4096 | LLM context window size |

Changing the embedding model or dimension requires re-uploading documents. Stored chunks record both; incompatible queries are rejected. Database passwords used in DATABASE_URL must be URL-safe or percent-encoded there.

Compose constructs DATABASE_URL for Python and passes POSTGRES_PASSWORD to Spring. When running Python outside Compose, explicitly configure DATABASE_URL; imports still work without it, but Settings construction rejects missing/blank values. Spring likewise requires a runtime POSTGRES_PASSWORD instead of a committed fallback.

### Rotate an existing database password

Changing .env does not change the PostgreSQL role password in an existing volume. Generate a new password locally, put it in the ignored .env, then connect to the still-running database before recreating containers:

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

At the psql prompt, enter `\password`, supply the same new password from .env twice, then enter `\q`. This updates the current database role without writing the plaintext password into a SQL command. Recreate services with `docker compose up --build -d` and verify an upload/question works.

Rotate any committed password wherever it was used; updating source does not erase Git history or invalidate old credentials. Keep PostgreSQL/model volumes. Do not mark a secret-scanner alert revoked until the old password no longer authenticates. Do not share `docker compose config` output, since resolved configuration includes credentials; use `docker compose config --quiet` for validation.

## Grounding and scope

The chain instructs the model to answer only from retrieved excerpts and to ignore instructions embedded in documents. When no relevant chunk passes the threshold, it returns a fixed insufficient-information answer without calling the chat model.

Sources represent the retrieved context, not an independent factual verification. Small local models can still make mistakes or cite incorrectly. Inspect excerpts, try a larger LLM if resources permit, and tune the retrieval threshold for your documents.

This is a local learning backend. Each request concerns one document; multiple document IDs can coexist and retrieval is isolated. Authentication, OCR, conversations, a UI, queues, and object storage are outside this V1.

## Stop, rebuild, and troubleshoot

```bash
docker compose logs --tail=100 backend ai-service postgres ollama-init
docker compose down
docker compose up --build -d
```

Normal down preserves PostgreSQL and Ollama volumes. Keep volumes when restarting; deleting them removes uploaded data and downloaded models.

If startup is waiting, inspect ollama-init logs. Model downloads require internet access; retry with docker compose up -d after resolving connectivity.

If you get 503, inspect Python/PostgreSQL/Ollama logs. If a model is absent, run docker compose run --rm ollama-init. If an upload times out, use a smaller document or increase both timeouts. Processing is synchronous; do not automatically retry an uncertain upload because it creates a new document.

If port 8080 is occupied, change BACKEND_PORT and use the new port. PDF scans require OCR, which is not included. UTF-8 is required for TXT.

For schema and ownership details see [architecture](docs/architecture.md) and [database notes](database/README.md).
