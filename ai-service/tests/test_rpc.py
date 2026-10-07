from uuid import uuid4

import grpc
import pytest

from app.generated import document_ai_pb2 as pb
from app.grpc_server import DocumentAIService
from app.retrieval.vector_store import Source


class Context:
    def abort(self, code, detail):
        raise RpcFailure(code, detail)

    def is_active(self):
        return True


class RpcFailure(Exception):
    def __init__(self, code, detail):
        self.code = code
        super().__init__(detail)


class Processor:
    def process(self, document_id, filename, content, active):
        assert content == b"text"
        return 2


class Chain:
    def ask(self, doc_id, question):
        return "answer", [Source(0, None, "text", 0.9)]


def test_rpc_processing_and_source_mapping():
    service = DocumentAIService(Processor(), Chain())
    response = service.ProcessDocument(pb.ProcessDocumentRequest(
        document_id=str(uuid4()), filename="x.txt", file_content=b"text"), Context())
    assert response.chunk_count == 2
    answer = service.AskQuestion(pb.QuestionRequest(document_id=str(uuid4()), question="why?"), Context())
    assert answer.answer == "answer"
    assert not answer.sources[0].HasField("page")


def test_invalid_content_maps_to_invalid_argument():
    class BadProcessor:
        def process(self, *args):
            raise ValueError("bad PDF")
    with pytest.raises(RpcFailure) as error:
        DocumentAIService(BadProcessor(), Chain()).ProcessDocument(
            pb.ProcessDocumentRequest(document_id=str(uuid4()), filename="x.pdf"), Context())
    assert error.value.code == grpc.StatusCode.INVALID_ARGUMENT


def test_model_outage_maps_to_unavailable_without_leaking_internals():
    class BrokenChain:
        def ask(self, *args):
            raise ConnectionError("secret credentials")
    with pytest.raises(RpcFailure) as error:
        DocumentAIService(Processor(), BrokenChain()).AskQuestion(
            pb.QuestionRequest(document_id=str(uuid4()), question="why?"), Context())
    assert error.value.code == grpc.StatusCode.UNAVAILABLE
    assert "secret" not in str(error.value)
