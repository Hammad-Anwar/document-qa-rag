from dataclasses import dataclass
import os


@dataclass(frozen=True)
class Settings:
    database_url: str = os.getenv("DATABASE_URL", "postgresql://documentqa:documentqa@localhost:5432/documentqa")
    ollama_url: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
    embedding_model: str = os.getenv("EMBEDDING_MODEL", "nomic-embed-text")
    llm_model: str = os.getenv("LLM_MODEL", "llama3.2:3b")
    dimensions: int = int(os.getenv("EMBEDDING_DIMENSIONS", "768"))
    timeout: int = int(os.getenv("MODEL_TIMEOUT_SECONDS", "240"))
    top_k: int = int(os.getenv("RETRIEVAL_TOP_K", "4"))
    min_similarity: float = float(os.getenv("RETRIEVAL_MIN_SIMILARITY", "0.35"))

    def __post_init__(self):
        if self.dimensions < 1 or self.timeout < 1 or not 1 <= self.top_k <= 20:
            raise ValueError("Invalid model or retrieval configuration")
        if not -1 <= self.min_similarity <= 1:
            raise ValueError("Similarity threshold must be between -1 and 1")
