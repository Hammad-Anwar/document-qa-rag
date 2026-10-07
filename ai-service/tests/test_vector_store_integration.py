"""Run with TEST_DATABASE_URL against the migrated local database."""
import os
from uuid import uuid4

import pytest

from app.database.connection import Database
from app.document.splitter import Chunk
from app.retrieval.vector_store import VectorStore


pytestmark = pytest.mark.skipif(not os.getenv("TEST_DATABASE_URL"), reason="Live pgvector database not configured")


@pytest.fixture
def records():
    database = Database(os.environ["TEST_DATABASE_URL"])
    ids = [str(uuid4()), str(uuid4())]
    with database.connect() as connection:
        for doc_id in ids:
            connection.execute("INSERT INTO documents (id, filename, file_type, file_size, status) VALUES (%s, 'integration.txt', 'TXT', 1, 'PROCESSING')", (doc_id,))
    try:
        yield database, VectorStore(database), ids
    finally:
        with database.connect() as connection:
            for doc_id in ids:
                connection.execute("DELETE FROM documents WHERE id = %s", (doc_id,))


def test_pgvector_similarity_never_leaks_another_document(records):
    database, store, ids = records
    store.replace(ids[0], [Chunk(0, 1, "TCP"), Chunk(1, 2, "UDP")], [[1, 0], [0, 1]], "test-model")
    store.replace(ids[1], [Chunk(0, 9, "private other document")], [[1, 0]], "test-model")
    with database.connect() as connection:
        for doc_id in ids:
            connection.execute("UPDATE documents SET status = 'READY' WHERE id = %s", (doc_id,))
    sources = store.search(ids[0], [1, 0], "test-model", 4, 0.5)
    assert len(sources) == 1
    assert sources[0].content == "TCP"
    assert sources[0].page == 1
    assert sources[0].similarity == pytest.approx(1.0)
    assert store.search(ids[0], [-1, 0], "test-model", 4, 0.5) == []


def test_replace_rolls_back_on_failed_insert(records):
    database, store, ids = records
    store.replace(ids[0], [Chunk(0, None, "original")], [[1, 0]], "test-model")
    with pytest.raises(Exception):
        store.replace(ids[0], [Chunk(0, None, "duplicate A"), Chunk(0, None, "duplicate B")],
                      [[1, 0], [1, 0]], "test-model")
    with database.connect() as connection:
        result = connection.execute("SELECT content FROM document_chunks WHERE document_id = %s", (ids[0],)).fetchall()
    assert result == [("original",)]


def test_changed_embedding_model_requires_reupload(records):
    database, store, ids = records
    store.replace(ids[0], [Chunk(0, None, "text")], [[1, 0]], "old-model")
    with database.connect() as connection:
        connection.execute("UPDATE documents SET status = 'READY' WHERE id = %s", (ids[0],))
    with pytest.raises(ValueError, match="model changed"):
        store.search(ids[0], [1, 0], "new-model", 4, 0.5)


def test_late_processing_cannot_repopulate_a_failed_document(records):
    database, store, ids = records
    with database.connect() as connection:
        connection.execute("UPDATE documents SET status = 'FAILED' WHERE id = %s", (ids[0],))
    with pytest.raises(ValueError, match="not available"):
        store.replace(ids[0], [Chunk(0, None, "late write")], [[1, 0]], "test-model")
    with database.connect() as connection:
        assert connection.execute("SELECT count(*) FROM document_chunks WHERE document_id = %s",
                                  (ids[0],)).fetchone()[0] == 0
