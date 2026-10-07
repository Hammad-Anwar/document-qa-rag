from io import BytesIO
from pathlib import PurePath

from pypdf import PdfReader

MAX_BYTES = 10 * 1024 * 1024
MAX_CHARACTERS = 2_000_000
MAX_PAGES = 500


def load_document(filename: str, data: bytes) -> list[tuple[int | None, str]]:
    if not data or len(data) > MAX_BYTES:
        raise ValueError("File must contain between 1 byte and 10 MiB")
    suffix = PurePath(filename).suffix.lower()
    if suffix == ".txt":
        try:
            text = data.decode("utf-8-sig")
        except UnicodeDecodeError as exc:
            raise ValueError("TXT files must use UTF-8 encoding") from exc
        if "\x00" in text:
            raise ValueError("TXT files must contain plain text")
        pages = [(None, text.strip())]
    elif suffix == ".pdf":
        try:
            reader = PdfReader(BytesIO(data))
            if reader.is_encrypted:
                raise ValueError("Encrypted PDFs are not supported")
            if len(reader.pages) > MAX_PAGES:
                raise ValueError("PDF exceeds the 500-page limit")
            pages = []
            characters = 0
            for index, page in enumerate(reader.pages, start=1):
                text = (page.extract_text() or "").strip()
                characters += len(text)
                if characters > MAX_CHARACTERS:
                    raise ValueError("Document exceeds the extracted text limit")
                if text:
                    pages.append((index, text))
        except ValueError:
            raise
        except Exception as exc:
            raise ValueError("PDF could not be read") from exc
    else:
        raise ValueError("Only PDF and TXT files are supported")
    if not any(text for _, text in pages):
        raise ValueError("Document contains no extractable text; scanned PDFs require OCR")
    if sum(len(text) for _, text in pages) > MAX_CHARACTERS:
        raise ValueError("Document exceeds the extracted text limit")
    return pages
