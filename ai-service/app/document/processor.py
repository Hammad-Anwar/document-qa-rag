from uuid import UUID

from app.document.loader import load_document
from app.document.splitter import split_pages
from app.embeddings.embedding_service import validate_vectors


class DocumentProcessor:
    def __init__(self, embeddings, store, model: str, dimensions: int):
        self.embeddings = embeddings
        self.store = store
        self.model = model
        self.dimensions = dimensions

    def process(self, document_id: str, filename: str, content: bytes, active=lambda: True) -> int:
        UUID(document_id)
        chunks = split_pages(load_document(filename, content))
        vectors = []
        for start in range(0, len(chunks), 32):
            if not active():
                raise TimeoutError("Processing request was cancelled")
            batch = chunks[start:start + 32]
            result = self.embeddings.embed_documents([c.content for c in batch])
            validate_vectors(result, self.dimensions, len(batch))
            vectors.extend(result)
        if not active():
            raise TimeoutError("Processing request was cancelled")
        self.store.replace(document_id, chunks, vectors, self.model)
        return len(chunks)
