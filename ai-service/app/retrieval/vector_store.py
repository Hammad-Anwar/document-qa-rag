from dataclasses import dataclass
from uuid import UUID

import numpy as np


@dataclass(frozen=True)
class Source:
    chunk_index: int
    page: int | None
    content: str
    similarity: float


class VectorStore:
    def __init__(self, database):
        self.database = database

    def replace(self, document_id, chunks, embeddings, model):
        doc_id = UUID(document_id)
        with self.database.connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute("SELECT status FROM documents WHERE id = %s FOR UPDATE", (doc_id,))
                row = cursor.fetchone()
                if not row or row[0] != "PROCESSING":
                    raise ValueError("Document is not available for processing")
                cursor.execute("DELETE FROM document_chunks WHERE document_id = %s", (doc_id,))
                cursor.executemany(
                    """INSERT INTO document_chunks
                       (document_id, chunk_index, page, content, embedding, embedding_model, dimensions)
                       VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                    [(doc_id, c.index, c.page, c.content, np.asarray(v, dtype=np.float32), model, len(v))
                     for c, v in zip(chunks, embeddings, strict=True)],
                )

    def search(self, document_id, vector, model, top_k, min_similarity):
        doc_id = UUID(document_id)
        query = np.asarray(vector, dtype=np.float32)
        with self.database.connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute("SELECT status FROM documents WHERE id = %s", (doc_id,))
                row = cursor.fetchone()
                if not row or row[0] != "READY":
                    raise ValueError("Document is not ready")
                cursor.execute(
                    "SELECT count(*) FROM document_chunks WHERE document_id = %s AND (embedding_model <> %s OR dimensions <> %s)",
                    (doc_id, model, len(vector)),
                )
                if cursor.fetchone()[0]:
                    raise ValueError("Embedding model changed; re-upload the document")
                cursor.execute(
                    """SELECT chunk_index, page, content, 1 - (embedding <=> %s) AS similarity
                       FROM document_chunks WHERE document_id = %s AND embedding_model = %s AND dimensions = %s
                       AND 1 - (embedding <=> %s) >= %s
                       ORDER BY embedding <=> %s LIMIT %s""",
                    (query, doc_id, model, len(vector), query, min_similarity, query, top_k),
                )
                return [Source(*row) for row in cursor.fetchall()]
