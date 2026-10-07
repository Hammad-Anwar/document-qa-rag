# Flyway migrations

V1__document_qa.sql creates the pgvector extension and documents, document_chunks, and questions tables. Spring applies migrations at startup. Add versioned migrations for schema changes; do not edit migrations already applied to a persistent database.
