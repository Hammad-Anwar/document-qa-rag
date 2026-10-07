from app.generated.document_ai_pb2 import PingRequest
from app.grpc_server import DocumentAIService


def test_ping_returns_ai_service_greeting() -> None:
    response = DocumentAIService().Ping(PingRequest(name="Spring Boot"), None)

    assert response.message == "Hello Spring Boot from Python AI service"
