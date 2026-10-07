from uuid import UUID

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama

from app.embeddings.embedding_service import validate_vectors
from app.rag.prompts import NO_ANSWER, SYSTEM_PROMPT


class RagChain:
    def __init__(self, embeddings, store, model, embedding_model, dimensions,
                 top_k=4, min_similarity=0.35):
        self.embeddings = embeddings
        self.store = store
        self.embedding_model = embedding_model
        self.dimensions = dimensions
        self.top_k = top_k
        self.min_similarity = min_similarity
        prompt = ChatPromptTemplate.from_messages([
            ("system", SYSTEM_PROMPT),
            ("human", "Document excerpts:\n{context}\n\nQuestion: {question}"),
        ])
        self.chain = prompt | model | StrOutputParser()

    def ask(self, document_id, question):
        UUID(document_id)
        question = question.strip()
        if not question or len(question) > 2000:
            raise ValueError("Question must contain between 1 and 2000 characters")
        vector = self.embeddings.embed_query(question)
        validate_vectors([vector], self.dimensions, 1)
        sources = self.store.search(document_id, vector, self.embedding_model,
                                    self.top_k, self.min_similarity)
        if not sources:
            return NO_ANSWER, []
        context = "\n\n".join(f"[{i}] (page {s.page or 'TXT'}) {s.content}"
                               for i, s in enumerate(sources, 1))
        answer = self.chain.invoke({"context": context, "question": question}).strip()
        if not answer or NO_ANSWER.lower() in answer.lower():
            return NO_ANSWER, []
        return answer, sources


def create_chat_model(settings):
    return ChatOllama(model=settings.llm_model, base_url=settings.ollama_url,
                      temperature=0, num_predict=384,
                      num_ctx=settings.model_context_tokens,
                      client_kwargs={"timeout": settings.timeout})
