package com.example.documentqa.controller;

import com.example.documentqa.dto.*;
import com.example.documentqa.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Upload PDF/TXT files and ask questions about their contents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) { this.service = service; }

    @Operation(summary = "Upload a document",
        description = "Upload a PDF or UTF-8 TXT file (max 10 MiB). Processing is synchronous; "
            + "the response returns once parsing, embedding, and chunk storage complete.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Document processed and ready",
            content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Empty/unsupported file or invalid filename",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "413", description = "File exceeds 10 MiB",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Unreadable or empty-content PDF / invalid UTF-8",
            content = @Content(schema = @Schema(implementation = DocumentErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "AI or database dependency unavailable",
            content = @Content(schema = @Schema(implementation = DocumentErrorResponse.class))),
        @ApiResponse(responseCode = "504", description = "AI processing deadline exceeded",
            content = @Content(schema = @Schema(implementation = DocumentErrorResponse.class))),
    })
    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
        @Parameter(description = "PDF or TXT file to upload", required = true)
        @RequestPart("file") MultipartFile file) {
        return service.upload(file);
    }

    @Operation(summary = "Get document status",
        description = "Retrieve metadata and processing status for a previously uploaded document.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document metadata",
            content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Malformed UUID",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Document not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    })
    @GetMapping("/{id}")
    public DocumentResponse get(
        @Parameter(description = "Document UUID", required = true)
        @PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(summary = "Ask a question",
        description = "Ask a 1–2000 character question about a READY document. "
            + "Returns the answer with retrieved source excerpts and saves to history.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Answer generated",
            content = @Content(schema = @Schema(implementation = QuestionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid question body or UUID",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Document not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Document is not READY",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "AI dependency unavailable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "504", description = "AI RPC deadline exceeded",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    })
    @PostMapping("/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse ask(
        @Parameter(description = "Document UUID", required = true)
        @PathVariable UUID id,
        @Valid @RequestBody QuestionRequest request) {
        return service.ask(id, request.question());
    }

    @Operation(summary = "Question history",
        description = "Retrieve saved question/answer pairs for a document in chronological order.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Question history",
            content = @Content(schema = @Schema(implementation = QuestionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters or UUID",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Document not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    })
    @GetMapping("/{id}/questions")
    public List<QuestionResponse> history(
        @Parameter(description = "Document UUID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "Maximum results (1–100)", example = "50")
        @RequestParam(defaultValue = "50") int limit,
        @Parameter(description = "Results to skip (non-negative)", example = "0")
        @RequestParam(defaultValue = "0") int offset) {
        return service.history(id, limit, offset);
    }
}
