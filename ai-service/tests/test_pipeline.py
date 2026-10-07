from uuid import uuid4

import pytest
from langchain_core.language_models.fake_chat_models import FakeListChatModel

from app.document.processor import DocumentProcessor
from app.document.splitter import Chunk
from app.rag.chain import RagChain, NO_ANSWER
from app.retrieval.vector_store import Source


class Embeddings:
    def embed_documents(self, texts):
        return [[1.0, 0.5] for _ in texts]

    def embed_query(self, text):
        return [1.0, 0.5]


class Store:
    def __init__(self):
        self.saved = None
        self.searched_id = None
        self.results = []

    def replace(self, document_id, chunks, embeddings, model):
        self.saved = (document_id, chunks, embeddings, model)

    def search(self, document_id, vector, model, top_k, min_similarity):
        self.searched_id = document_id
        return self.results


def test_processing_embeds_and_stores_the_requested_document():
    store = Store()
    doc_id = str(uuid4())
    count = DocumentProcessor(Embeddings(), store, "test", 2).process(
        doc_id, "notes.txt", b"TCP provides reliable delivery."
    )
    assert count == 1
    assert store.saved[0] == doc_id
    assert store.saved[1][0].content == "TCP provides reliable delivery."
    assert store.saved[2] == [[1.0, 0.5]]


def test_embedding_mismatch_does_not_save_partial_chunks():
    store = Store()
    with pytest.raises(ValueError, match="dimension"):
        DocumentProcessor(Embeddings(), store, "test", 768).process(
            str(uuid4()), "x.txt", b"some text"
        )
    assert store.saved is None


def test_empty_retrieval_refuses_without_invoking_model():
    store = Store()
    chain = RagChain(Embeddings(), store, FakeListChatModel(responses=[]), "test", 2)
    answer, sources = chain.ask(str(uuid4()), "What is TCP?")
    assert answer == NO_ANSWER
    assert sources == []


def test_rag_filters_by_id_and_returns_retrieved_sources():
    store = Store()
    store.results = [Source(0, 3, "TCP provides reliable delivery.", 0.9)]
    doc_id = str(uuid4())
    chain = RagChain(Embeddings(), store,
                     FakeListChatModel(responses=["TCP provides reliable delivery. [1]"]), "test", 2)
    answer, sources = chain.ask(doc_id, "What is TCP?")
    assert store.searched_id == doc_id
    assert "reliable" in answer
    assert sources[0].page == 3


def test_invalid_vector_is_rejected():
    from app.embeddings.embedding_service import validate_vectors
    with pytest.raises(ValueError):
        validate_vectors([[float("nan"), 0]], 2, 1)
    with pytest.raises(ValueError):
        validate_vectors([[0, 0]], 2, 1)
    with pytest.raises(ValueError):
        validate_vectors([], 2, 1)


@pytest.mark.parametrize("response", ["", NO_ANSWER, NO_ANSWER + " [1]"])
def test_model_refusal_does_not_claim_supporting_sources(response):
    store = Store()
    store.results = [Source(0, None, "TCP provides reliable delivery.", 0.9)]
    chain = RagChain(Embeddings(), store, FakeListChatModel(responses=[response]), "test", 2)
    assert chain.ask(str(uuid4()), "Who invented TCP?") == (NO_ANSWER, [])


def test_cancelled_processing_never_saves_chunks():
    store = Store()
    with pytest.raises(TimeoutError):
        DocumentProcessor(Embeddings(), store, "test", 2).process(
            str(uuid4()), "x.txt", b"some text", active=lambda: False)
    assert store.saved is None
