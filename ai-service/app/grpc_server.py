from __future__ import annotations

import logging
from concurrent import futures

import grpc

from app.config import Settings
from app.database.connection import Database
from app.document.processor import DocumentProcessor
from app.embeddings.embedding_service import create_embeddings
from app.rag.chain import RagChain, create_chat_model
from app.retrieval.vector_store import VectorStore

from app.generated import document_ai_pb2, document_ai_pb2_grpc


LOGGER = logging.getLogger(__name__)


class DocumentAIService(document_ai_pb2_grpc.DocumentAIServiceServicer):
    def __init__(self, processor=None, chain=None):
        self.processor = processor
        self.chain = chain

    def Ping(self, request, context):  # noqa: N802 - gRPC defines this name
        name = request.name.strip() or "Spring Boot"
        return document_ai_pb2.PingResponse(
            message=f"Hello {name} from Python AI service"
        )

    def ProcessDocument(self, request, context):
        try:
            count = self.processor.process(request.document_id, request.filename,
                                           request.file_content, context.is_active)
            return document_ai_pb2.ProcessDocumentResponse(chunk_count=count)
        except ValueError as exc:
            context.abort(grpc.StatusCode.INVALID_ARGUMENT, str(exc))
        except TimeoutError:
            context.abort(grpc.StatusCode.DEADLINE_EXCEEDED, "Document processing timed out")
        except Exception:
            LOGGER.exception("Document processing failed")
            context.abort(grpc.StatusCode.UNAVAILABLE, "AI processing dependency is unavailable")

    def AskQuestion(self, request, context):
        try:
            answer, sources = self.chain.ask(request.document_id, request.question)
            result = document_ai_pb2.QuestionResponse(answer=answer)
            for source in sources:
                entry = result.sources.add(chunk_index=source.chunk_index,
                    content=source.content, similarity=source.similarity)
                if source.page is not None:
                    entry.page = source.page
            return result
        except ValueError as exc:
            context.abort(grpc.StatusCode.INVALID_ARGUMENT, str(exc))
        except Exception:
            LOGGER.exception("Question answering failed")
            context.abort(grpc.StatusCode.UNAVAILABLE, "AI answering dependency is unavailable")


def serve(port: int) -> None:
    settings = Settings()
    embeddings = create_embeddings(settings)
    store = VectorStore(Database(settings.database_url))
    processor = DocumentProcessor(embeddings, store, settings.embedding_model, settings.dimensions)
    chain = RagChain(embeddings, store, create_chat_model(settings), settings.embedding_model,
                     settings.dimensions, settings.top_k, settings.min_similarity)
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=4), options=[
        ("grpc.max_receive_message_length", 12 * 1024 * 1024),
        ("grpc.max_send_message_length", 12 * 1024 * 1024),
    ])
    document_ai_pb2_grpc.add_DocumentAIServiceServicer_to_server(
        DocumentAIService(processor, chain), server
    )
    if server.add_insecure_port(f"[::]:{port}") == 0:
        raise RuntimeError("Could not bind gRPC port")
    server.start()
    LOGGER.info("Python AI gRPC service listening on port %s", port)
    server.wait_for_termination()
