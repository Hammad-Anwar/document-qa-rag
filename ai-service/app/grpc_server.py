from __future__ import annotations

import logging
from concurrent import futures

import grpc

from app.generated import document_ai_pb2, document_ai_pb2_grpc


LOGGER = logging.getLogger(__name__)


class DocumentAIService(document_ai_pb2_grpc.DocumentAIServiceServicer):
    """Minimal gRPC service used to verify Java-to-Python communication."""

    def Ping(self, request, context):  # noqa: N802 - gRPC defines this name
        name = request.name.strip() or "Spring Boot"
        return document_ai_pb2.PingResponse(
            message=f"Hello {name} from Python AI service"
        )


def serve(port: int) -> None:
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=4))
    document_ai_pb2_grpc.add_DocumentAIServiceServicer_to_server(
        DocumentAIService(), server
    )
    server.add_insecure_port(f"[::]:{port}")
    server.start()
    LOGGER.info("Python AI gRPC service listening on port %s", port)
    server.wait_for_termination()
