# RAG

prompts.py defines the context-only instruction and insufficient-information answer. chain.py composes the LangChain prompt, ChatOllama, and output parser. It skips generation for empty retrieval and returns actual retrieved excerpts as sources.
