# Python database adapter

connection.py opens bounded PostgreSQL connections, registers the pgvector adapter, and sets a statement timeout. Python owns document_chunks; Spring owns document metadata/question history and Flyway migrations.
