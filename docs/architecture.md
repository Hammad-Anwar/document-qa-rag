# Architecture

```text
HTTP client
   |
Spring Boot REST API
   |-- JDBC/Flyway --> PostgreSQL: documents, questions
   |
   | gRPC (Protocol Buffers)
   v
Python AI service
   |-- pypdf / UTF-8 loader --> LangChain text splitter
   |-- OllamaEmbeddings --> PostgreSQL/pgvector: document_chunks
   |-- document-ID-filtered retrieval --> LangChain prompt --> ChatOllama
```

The public backend uses Java 21/Spring Boot. Python owns AI work. PostgreSQL stores metadata, question history, and vectors. Four long-running containers are started, plus an Ollama model-initialization container that exits on success.

## Persistence and ownership

Spring runs V1__document_qa.sql through Flyway, creating the vector extension and all three tables. Spring owns writes to documents/questions; Python writes document_chunks. Chunk and question rows reference documents and cascade when a document is explicitly removed from the database.

Python replaces chunks in one transaction while locking the document metadata row. It accepts only PROCESSING documents. Spring commits metadata before the RPC. Success sets READY; failure sets FAILED and removes any chunks. There is no long database transaction spanning model calls.

A sudden process/host crash during an upload can leave PROCESSING metadata. This V1 does not have background jobs or automatic recovery; inspect the record and upload the file again. Existing metadata remains available.

## Vector search

Every chunk includes its document ID, page, chunk index, text, embedding model, dimensions, and vector. Search uses pgvector cosine distance, filters by the requested document ID/model/dimension, applies a similarity threshold, and returns up to top_k chunks.

The vector column is dimension-flexible with a database check matching its dimensions field. The Python adapter rejects zero, non-finite, or unexpected-length vectors before persistence. This allows configured model sizes without silently mixing incompatible embeddings. V1 uses exact search; no approximate index is needed for small uploads.

## gRPC

The shared proto retains Ping and adds ProcessDocument and AskQuestion. Java and Python bindings are generated during builds. The message size allowance is 12 MiB for a maximum 10 MiB file. Ping has a three-second deadline; processing/questions use a configurable deadline (default 300 seconds).

Validation errors become gRPC INVALID_ARGUMENT and HTTP 422. Availability errors become HTTP 503; RPC deadlines become HTTP 504. Python does not expose dependency exception details to callers.

## RAG

Loaders preserve one-based PDF pages; TXT has no page. LangChain splits each page using 1000-character chunks with 200-character overlap. Embeddings are batched. Questions are embedded using the same configured model. The LangChain chain composes a chat prompt, ChatOllama, and StrOutputParser.

The prompt treats document content as untrusted context and asks for bracketed citations. Empty retrieval skips generation and returns the insufficient-information answer. Sources are the actual retrieved excerpts. Prompt-based grounding is not a guarantee of factual accuracy.

## Runtime

Docker keeps PostgreSQL, Ollama models, and embeddings in named persistent volumes. Only Spring's HTTP port is exposed, on 127.0.0.1. Model initialization completes before Python starts. Spring applies migrations during startup. A gRPC health check confirms Python serves Ping.

See the README for run/test commands and configuration.
