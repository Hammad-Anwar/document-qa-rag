# API reference

Base URL: http://localhost:8080

## Upload

POST /api/documents, multipart/form-data, required part file.

```bash
curl -F "file=@examples/networking.txt" http://localhost:8080/api/documents
```

Success: 201 with id, filename, fileType, fileSize, status READY, chunkCount, errorMessage null, createdAt. Upload waits for parsing, embedding, and chunk storage.

Validation: empty/unsupported filename or type -> 400; over 10 MiB -> 413. Unreadable/empty-content PDFs or invalid UTF-8 -> 422 with documentId. Scanned or encrypted PDFs are not supported. Maximum 500 pages, 2 million extracted characters, 3000 chunks.

A processing/model failure preserves FAILED metadata. Response includes error, message, documentId. Use GET to inspect that record.

## Get status

GET /api/documents/{id} -> 200 document metadata. Unknown ID -> 404. Malformed UUID -> 400.

## Ask

POST /api/documents/{id}/questions, application/json:

```json
{"question":"How does TCP provide reliable delivery?"}
```

Success: 201:

```json
{
  "id": "<question-uuid>",
  "documentId": "<document-uuid>",
  "question": "How does TCP provide reliable delivery?",
  "answer": "TCP uses acknowledgements and retransmissions. [1]",
  "sources": [
    {"chunkIndex":0,"page":null,"content":"TCP provides reliable delivery using acknowledgements and retransmissions.","similarity":0.8}
  ],
  "createdAt":"<timestamp>"
}
```

Question must have 1–2000 non-whitespace characters. Invalid body -> 400. Missing document -> 404. Document not READY -> 409.

If retrieval is insufficient, answer is:
I could not find sufficient information in the uploaded document to answer this question.

Sources are retrieved context. Page is null for TXT and one-based for PDF. ChunkIndex is zero-based. History stores exactly the returned sources.

## History

GET /api/documents/{id}/questions?limit=50&offset=0 -> 200 JSON array in chronological order. Default limit 50; valid 1–100. Offset must be non-negative.

## Health

GET /api/health/ai -> 200 with status UP and a Python greeting. Tests gRPC connectivity; does not independently validate model quality or database health.

## Errors

Standard errors contain error and message; upload processing errors additionally contain documentId.

| Status | Meaning |
| --- | --- |
| 400 | Invalid file, question, UUID, JSON, or pagination |
| 404 | Missing document |
| 409 | Document not READY |
| 413 | Upload too large |
| 422 | Unreadable content or incompatible embedding model |
| 503 | AI/model/database dependency unavailable |
| 504 | AI RPC deadline exceeded |

There is no authentication in local V1. Avoid exposing this API publicly.
