# Document processing

loader.py validates and extracts PDF/TXT text in memory. splitter.py uses LangChain with 1000-character chunks and 200-character overlap, preserving PDF pages. processor.py batches embeddings and delegates atomic storage. Scanned/encrypted PDFs and unusable text are rejected.
