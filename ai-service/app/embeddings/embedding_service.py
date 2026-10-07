import math

from langchain_ollama import OllamaEmbeddings


def validate_vectors(vectors, dimensions: int, expected_count: int) -> None:
    if len(vectors) != expected_count:
        raise ValueError("Embedding count does not match chunk count")
    for vector in vectors:
        if len(vector) != dimensions:
            raise ValueError("Embedding dimension does not match configured dimensions")
        if not all(math.isfinite(value) for value in vector) or not any(vector):
            raise ValueError("Embedding contains non-finite values or a zero vector")


def create_embeddings(settings):
    return OllamaEmbeddings(
        model=settings.embedding_model,
        base_url=settings.ollama_url,
        client_kwargs={"timeout": settings.timeout},
    )
