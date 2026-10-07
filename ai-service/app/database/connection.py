from contextlib import contextmanager

import psycopg
from pgvector.psycopg import register_vector


class Database:
    def __init__(self, url: str):
        self.url = url

    @contextmanager
    def connect(self):
        with psycopg.connect(self.url, connect_timeout=5) as connection:
            register_vector(connection)
            with connection.cursor() as cursor:
                cursor.execute("SET LOCAL statement_timeout = '15s'")
            yield connection
