# Docker runtime

Compose runs backend, ai-service, PostgreSQL/pgvector, and Ollama. ollama-init pulls configured models and exits successfully before AI startup. Both Dockerfiles use the repository as build context so they can consume the shared proto.

Only the backend is exposed on localhost. Named PostgreSQL/model volumes preserve state. Normal docker compose down does not delete these volumes. See the main README for startup, logs, tests, and configuration.
