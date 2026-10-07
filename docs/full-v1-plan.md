# Document QA RAG V1

The user's October 4 request authorizes completing the supplied V1 architecture in this repository. Work proceeds inline, preserving the existing staged ping scaffold.

## Contract and data flow

Spring owns document metadata/status and question history through JDBC and Flyway. Python owns parsing, LangChain splitting, Ollama embeddings, pgvector retrieval, and a LangChain prompt/model chain. Both use one PostgreSQL database. Original uploads are processed in memory and are not retained.

Uploads are synchronous, limited to 10 MiB PDF or UTF-8 TXT, and create a committed PROCESSING record before the Python RPC. Success marks READY; invalid content or dependency failure marks FAILED and returns the ID. Failed records remain inspectable. A question requires an existing READY document and 1–2000 non-whitespace characters.

The shared protobuf adds ProcessDocument and AskQuestion while retaining Ping. Upload RPC payloads allow 12 MiB and use a configurable 300-second deadline; questions use the same deadline. Python validates IDs and input, atomically replaces a document's chunks, filters all retrieval by document ID, and returns retrieved source indices/pages/content/similarity. Model names and vector dimensions are stored per chunk so mismatched models cannot silently be queried.

Sources describe retrieved context, not an independently verified proof of each generated claim. The prompt treats documents as untrusted data, instructs the model to answer only from context, and refuses when retrieval is empty or below the configurable similarity threshold. Model grounding remains probabilistic.

## Execution checklist

- [ ] Extend protobuf and database migrations.
- [ ] Test and implement Python loaders, splitting, embedding validation, transactional storage, retrieval, grounded chain, and RPC error mapping.
- [ ] Test and implement Spring upload/status/question/history APIs and persistent states.
- [ ] Configure Compose with pgvector, Ollama, model bootstrap, AI service, and backend; keep volumes and existing data recoverable.
- [ ] Run unit suites and real database/gRPC integration checks.
- [ ] Download small local models and exercise TXT/PDF upload, sources/history, document isolation, invalid requests, and dependency errors end to end.
- [ ] Replace milestone docs with current run/API/schema/troubleshooting instructions and root-folder guide.

## Local model defaults

Use llama3.2:3b for chat and nomic-embed-text for 768-dimensional embeddings. The initial 1B chat model repeatedly refused a clearly supported answer or added unsupported detail during live trials; the 3B model answered correctly under the unchanged grounding prompt. Exact dependency versions are pinned after resolving compatible packages. Compose performs model initialization before Python starts. An embedding model/dimension change requires re-uploading documents. Persistent PostgreSQL and Ollama volumes survive normal shutdown.

## Scope

Four requested document APIs plus the existing connectivity endpoint. No frontend, authentication, OCR, agents, cloud storage, or queue is included. This is a local learning application; ports bind to loopback and internal database/gRPC ports are not exposed.
