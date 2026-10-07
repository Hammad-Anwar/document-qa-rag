import io

import pytest

from app.document.loader import load_document
from app.document.splitter import split_pages


def test_txt_is_utf8_and_preserves_content():
    pages = load_document("notes.TXT", "TCP is reliable. Grüße".encode())
    assert pages == [(None, "TCP is reliable. Grüße")]


@pytest.mark.parametrize("name,data", [
    ("x.exe", b"text"), ("x.txt", b"\xff"), ("x.txt", b" \n "),
    ("x.pdf", b"not a PDF"), ("x.txt", b"text\x00"),
])
def test_rejects_unusable_documents(name, data):
    with pytest.raises(ValueError):
        load_document(name, data)


def test_blank_pdf_requires_ocr():
    from pypdf import PdfWriter
    writer = PdfWriter()
    writer.add_blank_page(width=100, height=100)
    data = io.BytesIO()
    writer.write(data)
    with pytest.raises(ValueError, match="text"):
        load_document("scan.pdf", data.getvalue())


def test_splitter_keeps_page_and_global_chunk_indices():
    chunks = split_pages([(1, "TCP reliable. " * 150), (2, "UDP connectionless.")])
    assert len(chunks) > 2
    assert [c.index for c in chunks] == list(range(len(chunks)))
    assert chunks[-1].page == 2
    assert all(len(c.content) <= 1000 for c in chunks)
