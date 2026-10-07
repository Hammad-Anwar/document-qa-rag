# Database

The authoritative schema is backend/src/main/resources/db/migration/V1__document_qa.sql and is applied by Spring Boot/Flyway. Do not maintain a second initialization script.

- documents: UUID, filename/type/size, PROCESSING/READY/FAILED status, chunk count, failure message, timestamp.
- document_chunks: document FK, zero-based chunk index, optional one-based PDF page, content, vector, embedding model/dimensions, timestamp.
- questions: UUID, document FK, question/answer, serialized source list, timestamp.

The document-ID index supports filtering exact cosine search. Vectors use a flexible dimension column with a dimension check; Python rejects model mismatches. All SQL values are parameterized.

Inspect the local database from the repository:

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT id, filename, status, chunk_count FROM documents;"'
```

The postgres_data volume survives docker compose down. Back up it before schema/data changes. Passwords in .env are for local development only.
