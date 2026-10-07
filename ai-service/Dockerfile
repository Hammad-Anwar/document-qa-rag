FROM python:3.12-slim

WORKDIR /workspace

COPY ai-service/requirements.txt /workspace/ai-service/requirements.txt
COPY ai-service/requirements.lock /workspace/ai-service/requirements.lock
RUN pip install --no-cache-dir -r /workspace/ai-service/requirements.lock

COPY proto /workspace/proto
COPY ai-service /workspace/ai-service
COPY scripts /workspace/scripts

RUN python -m grpc_tools.protoc \
      -I/workspace/proto \
      --python_out=/workspace/ai-service/app/generated \
      --grpc_python_out=/workspace/ai-service/app/generated \
      /workspace/proto/document_ai.proto

ENV PYTHONPATH=/workspace/ai-service:/workspace/ai-service/app/generated

EXPOSE 50051
CMD ["python", "-m", "app.main"]
