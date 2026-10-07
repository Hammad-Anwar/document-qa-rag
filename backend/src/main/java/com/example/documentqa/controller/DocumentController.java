package com.example.documentqa.controller;

import com.example.documentqa.dto.*;
import com.example.documentqa.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) { this.service = service; }

    @PostMapping(consumes="multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@RequestPart("file") MultipartFile file) { return service.upload(file); }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping("/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse ask(@PathVariable UUID id, @Valid @RequestBody QuestionRequest request) {
        return service.ask(id, request.question());
    }

    @GetMapping("/{id}/questions")
    public List<QuestionResponse> history(@PathVariable UUID id,
        @RequestParam(defaultValue="50") int limit, @RequestParam(defaultValue="0") int offset) {
        return service.history(id, limit, offset);
    }
}
