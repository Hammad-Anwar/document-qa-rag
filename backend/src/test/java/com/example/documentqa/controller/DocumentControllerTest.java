package com.example.documentqa.controller;

import com.example.documentqa.dto.*;
import com.example.documentqa.exception.*;
import com.example.documentqa.grpc.DocumentAiClient;
import com.example.documentqa.repository.DocumentRepository;
import com.example.documentqa.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentControllerTest {
    MockMvc mvc;
    JdbcTemplate jdbc;
    ObjectMapper mapper = new ObjectMapper();
    boolean failProcessing;
    RuntimeException processingError;
    RuntimeException answerError;
    boolean failStatusUpdates;
    UUID askedDocument;

    @BeforeEach
    void setup() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", ""));
        jdbc.execute("CREATE TABLE documents (id UUID PRIMARY KEY, filename VARCHAR(255), file_type VARCHAR(10), file_size BIGINT, status VARCHAR(20), chunk_count INT, error_message TEXT, created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE questions (id UUID PRIMARY KEY, document_id UUID REFERENCES documents(id), question TEXT, answer TEXT, sources TEXT, created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE document_chunks (document_id UUID REFERENCES documents(id))");
        DocumentAiClient ai = new DocumentAiClient() {
            public int process(UUID id, String filename, byte[] content) {
                if (processingError != null) throw processingError;
                if (failProcessing) throw new AiServiceUnavailableException("Python AI service is unavailable");
                return 2;
            }
            public AiAnswer ask(UUID id, String question) {
                if (answerError != null) throw answerError;
                askedDocument = id;
                return new AiAnswer("TCP provides reliable delivery. [1]",
                    List.of(new SourceResponse(0, 1, "TCP provides reliable delivery.", 0.9)));
            }
        };
        DocumentRepository repository = new DocumentRepository(jdbc, mapper) {
            @Override public void ready(UUID id, int count) {
                if (failStatusUpdates) throw new org.springframework.dao.DataAccessResourceFailureException("database offline");
                super.ready(id, count);
            }
            @Override public void failed(UUID id, String message) {
                if (failStatusUpdates) throw new org.springframework.dao.DataAccessResourceFailureException("database offline");
                super.failed(id, message);
            }
        };
        mvc = MockMvcBuilders.standaloneSetup(new DocumentController(
            new DocumentService(repository, ai)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    String upload() throws Exception {
        String body = mvc.perform(multipart("/api/documents").file(
                new MockMultipartFile("file", "notes.txt", "text/plain", "TCP reliable".getBytes())))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.chunkCount").value(2)).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asText();
    }

    @Test void uploadStatusQuestionAndHistoryPersist() throws Exception {
        String id = upload();
        mvc.perform(get("/api/documents/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.filename").value("notes.txt"));
        mvc.perform(post("/api/documents/" + id + "/questions").contentType("application/json")
                .content("{\"question\":\"What is TCP?\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.sources[0].page").value(1));
        assertEquals(UUID.fromString(id), askedDocument);
        mvc.perform(get("/api/documents/" + id + "/questions"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].question").value("What is TCP?"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM questions", Integer.class));
    }

    @Test void rejectsUnsupportedEmptyAndOversizeFilesBeforeSaving() throws Exception {
        for (MockMultipartFile file : List.of(
            new MockMultipartFile("file", "x.exe", "text/plain", new byte[]{1}),
            new MockMultipartFile("file", "x.txt", "text/plain", new byte[0]))) {
            mvc.perform(multipart("/api/documents").file(file)).andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/api/documents").file(new MockMultipartFile(
            "file", "x.txt", "text/plain", new byte[10 * 1024 * 1024 + 1])))
            .andExpect(status().isPayloadTooLarge());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM documents", Integer.class));
    }

    @Test void processingFailureIsPersistedAndCannotBeQuestioned() throws Exception {
        failProcessing = true;
        String body = mvc.perform(multipart("/api/documents").file(
            new MockMultipartFile("file", "x.txt", "text/plain", "text".getBytes())))
            .andExpect(status().isServiceUnavailable()).andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("documentId").asText();
        mvc.perform(get("/api/documents/" + id)).andExpect(jsonPath("$.status").value("FAILED"));
        mvc.perform(post("/api/documents/" + id + "/questions").contentType("application/json")
            .content("{\"question\":\"Why?\"}")).andExpect(status().isConflict());
    }

    @Test void validatesQuestionAndMissingDocument() throws Exception {
        String id = upload();
        mvc.perform(post("/api/documents/" + id + "/questions").contentType("application/json")
            .content("{\"question\":\"  \"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/documents/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/documents/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/documents/" + id + "/questions").contentType("application/json")
            .content("{\"question\":\"" + "x".repeat(2001) + "\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/documents/" + id + "/questions?limit=101"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/documents/" + id + "/questions?offset=-1"))
            .andExpect(status().isBadRequest());
    }

    @Test void aiQuestionFailureDoesNotPolluteHistory() throws Exception {
        String id = upload();
        answerError = new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
            "AI_SERVICE_UNAVAILABLE", "AI service unavailable");
        mvc.perform(post("/api/documents/" + id + "/questions").contentType("application/json")
            .content("{\"question\":\"What is TCP?\"}")).andExpect(status().isServiceUnavailable());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM questions", Integer.class));
        mvc.perform(get("/api/documents/" + id)).andExpect(jsonPath("$.status").value("READY"));
    }

    @Test void processingTimeoutReturnsIdAndRemovesOnlyFailedDocumentChunks() throws Exception {
        String readyId = upload();
        jdbc.update("INSERT INTO document_chunks (document_id) VALUES (?)", UUID.fromString(readyId));
        processingError = new ApiException(org.springframework.http.HttpStatus.GATEWAY_TIMEOUT,
            "AI_TIMEOUT", "AI service timed out");
        String body = mvc.perform(multipart("/api/documents").file(
            new MockMultipartFile("file", "late.txt", "text/plain", "text".getBytes())))
            .andExpect(status().isGatewayTimeout()).andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("documentId").asText();
        mvc.perform(get("/api/documents/" + id)).andExpect(jsonPath("$.status").value("FAILED"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM document_chunks", Integer.class));
    }

    @Test void databaseFailureAfterCreationStillReturnsCommittedDocumentId() throws Exception {
        failStatusUpdates = true;
        String body = mvc.perform(multipart("/api/documents").file(
            new MockMultipartFile("file", "offline.txt", "text/plain", "text".getBytes())))
            .andExpect(status().isServiceUnavailable()).andReturn().getResponse().getContentAsString();
        assertTrue(mapper.readTree(body).hasNonNull("documentId"), body);
        UUID id = UUID.fromString(mapper.readTree(body).get("documentId").asText());
        assertEquals("PROCESSING", jdbc.queryForObject("SELECT status FROM documents WHERE id = ?", String.class, id));
    }
}
