from __future__ import annotations

import logging
import os

from app.grpc_server import serve


def main() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s - %(message)s",
    )
    serve(int(os.getenv("GRPC_PORT", "50051")))


if __name__ == "__main__":
    main()
