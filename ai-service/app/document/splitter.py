from dataclasses import dataclass

from langchain_text_splitters import RecursiveCharacterTextSplitter


@dataclass(frozen=True)
class Chunk:
    index: int
    page: int | None
    content: str


def split_pages(pages: list[tuple[int | None, str]]) -> list[Chunk]:
    splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
    chunks = []
    for page, text in pages:
        for content in splitter.split_text(text):
            chunks.append(Chunk(len(chunks), page, content))
    if len(chunks) > 3000:
        raise ValueError("Document exceeds the 3000-chunk limit")
    return chunks
