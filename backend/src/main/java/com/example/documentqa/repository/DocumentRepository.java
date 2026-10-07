package com.example.documentqa.repository;

import com.example.documentqa.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public DocumentRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void create(UUID id, String filename, String type, long size) {
        jdbc.update("INSERT INTO documents (id, filename, file_type, file_size, status) VALUES (?, ?, ?, ?, 'PROCESSING')",
            id, filename, type, size);
    }

    public Optional<DocumentResponse> find(UUID id) {
        return jdbc.query("SELECT * FROM documents WHERE id = ?", (row, index) -> document(row), id)
            .stream().findFirst();
    }

    public void ready(UUID id, int count) {
        jdbc.update("UPDATE documents SET status = 'READY', chunk_count = ?, error_message = NULL WHERE id = ?",
            count, id);
    }

    public void failed(UUID id, String message) {
        // Locking the document row serializes this update with Python's chunk transaction.
        jdbc.update("UPDATE documents SET status = 'FAILED', error_message = ?, chunk_count = 0 WHERE id = ?",
            message, id);
        jdbc.update("DELETE FROM document_chunks WHERE document_id = ?", id);
    }

    public QuestionResponse saveQuestion(UUID docId, String question, AiAnswer answer) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO questions (id, document_id, question, answer, sources) VALUES (?, ?, ?, ?, ?)",
            id, docId, question, answer.answer(), writeSources(answer.sources()));
        return jdbc.queryForObject("SELECT * FROM questions WHERE id = ?", (row, index) -> question(row), id);
    }

    public List<QuestionResponse> history(UUID id, int limit, int offset) {
        return jdbc.query("SELECT * FROM questions WHERE document_id = ? ORDER BY created_at, id LIMIT ? OFFSET ?",
            (row, index) -> question(row), id, limit, offset);
    }

    private DocumentResponse document(ResultSet row) throws SQLException {
        return new DocumentResponse(row.getObject("id", UUID.class), row.getString("filename"),
            row.getString("file_type"), row.getLong("file_size"), row.getString("status"),
            row.getInt("chunk_count"), row.getString("error_message"),
            row.getObject("created_at", OffsetDateTime.class));
    }

    private QuestionResponse question(ResultSet row) throws SQLException {
        return new QuestionResponse(row.getObject("id", UUID.class), row.getObject("document_id", UUID.class),
            row.getString("question"), row.getString("answer"), readSources(row.getString("sources")),
            row.getObject("created_at", OffsetDateTime.class));
    }

    private String writeSources(List<SourceResponse> sources) {
        try { return mapper.writeValueAsString(sources); }
        catch (Exception exception) { throw new IllegalStateException("Could not store answer sources", exception); }
    }

    private List<SourceResponse> readSources(String value) {
        try { return mapper.readValue(value, new TypeReference<List<SourceResponse>>() {}); }
        catch (Exception exception) { throw new IllegalStateException("Could not read answer sources", exception); }
    }
}
