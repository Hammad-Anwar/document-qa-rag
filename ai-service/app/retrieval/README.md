# Retrieval

vector_store.py performs atomic chunk replacement and parameterized cosine-similarity queries with pgvector. Retrieval always filters by document ID and validates embedding model/dimensions. Exact search is used for small V1 documents.
