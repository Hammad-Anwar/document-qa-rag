package com.example.documentqa.service;

import com.example.documentqa.dto.*;
import com.example.documentqa.exception.*;
import com.example.documentqa.grpc.DocumentAiClient;
import com.example.documentqa.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentService.class);
    private final DocumentRepository repository;
    private final DocumentAiClient ai;
    public DocumentService(DocumentRepository repository, DocumentAiClient ai) {
        this.repository = repository;
        this.ai = ai;
    }

    public DocumentResponse upload(MultipartFile file) {
        if (file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Upload a non-empty file");
        if (file.getSize() > 10 * 1024 * 1024) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
            "FILE_TOO_LARGE", "Maximum file size is 10 MiB");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (name.isBlank() || name.length() > 255 || name.chars().anyMatch(Character::isISOControl))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILENAME", "Filename is invalid or too long");
        String lower = name.toLowerCase(Locale.ROOT);
        String type = lower.endsWith(".pdf") ? "PDF" : lower.endsWith(".txt") ? "TXT" : "";
        if (type.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE",
            "Only PDF and TXT files are supported");
        byte[] content;
        try { content = file.getBytes(); }
        catch (IOException exception) { throw new ApiException(HttpStatus.BAD_REQUEST, "UNREADABLE_FILE", "Upload could not be read"); }
        UUID id = UUID.randomUUID();
        repository.create(id, name, type, file.getSize());
        try {
            int count = ai.process(id, name, content);
            if (count < 1) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "EMPTY_DOCUMENT", "Document contains no chunks");
            repository.ready(id, count);
        } catch (ApiException exception) {
            markFailed(id, exception.getMessage());
            throw new ApiException(exception.status(), exception.error(), exception.getMessage(), id);
        } catch (RuntimeException exception) {
            markFailed(id, "Document processing failed");
            if (exception instanceof DataAccessException) throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE",
                "Database operation failed after upload creation; inspect document status", id);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DOCUMENT_PROCESSING_FAILED",
                "Document processing failed; inspect service logs", id);
        }
        return get(id);
    }

    private void markFailed(UUID id, String message) {
        try { repository.failed(id, message); }
        catch (RuntimeException exception) {
            // An unavailable database cannot guarantee FAILED persistence, but the caller keeps its ID.
            LOGGER.error("Could not clean up document {}; status may remain PROCESSING", id, exception);
        }
    }

    public DocumentResponse get(UUID id) {
        return repository.find(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
            "DOCUMENT_NOT_FOUND", "Document does not exist"));
    }

    public QuestionResponse ask(UUID id, String question) {
        DocumentResponse document = get(id);
        if (!"READY".equals(document.status())) throw new ApiException(HttpStatus.CONFLICT,
            "DOCUMENT_NOT_READY", "Document must have READY status before asking questions");
        if (question == null || question.isBlank() || question.length() > 2000)
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUESTION", "Question must contain 1–2000 characters");
        String text = question.strip();
        return repository.saveQuestion(id, text, ai.ask(id, text));
    }

    public List<QuestionResponse> history(UUID id, int limit, int offset) {
        get(id);
        if (limit < 1 || limit > 100 || offset < 0) throw new ApiException(HttpStatus.BAD_REQUEST,
            "INVALID_PAGINATION", "Limit must be 1–100 and offset must be non-negative");
        return repository.history(id, limit, offset);
    }
}
