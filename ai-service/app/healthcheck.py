import grpc
from app.generated import document_ai_pb2, document_ai_pb2_grpc


def main():
    with grpc.insecure_channel("localhost:50051") as channel:
        stub = document_ai_pb2_grpc.DocumentAIServiceStub(channel)
        stub.Ping(document_ai_pb2.PingRequest(name="healthcheck"), timeout=3)


if __name__ == "__main__":
    main()
