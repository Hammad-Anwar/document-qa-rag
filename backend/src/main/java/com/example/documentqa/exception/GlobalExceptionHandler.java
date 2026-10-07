package com.example.documentqa.exception;

import com.example.documentqa.dto.ApiErrorResponse;
import com.example.documentqa.dto.DocumentErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<DocumentErrorResponse> handleApi(ApiException exception) {
        return ResponseEntity.status(exception.status()).body(new DocumentErrorResponse(
            exception.error(), exception.getMessage(), exception.documentId()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestPartException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiErrorResponse> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse("INVALID_REQUEST",
            "Check the document ID, file part, and question (1–2000 characters)"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponse> handleSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new ApiErrorResponse("FILE_TOO_LARGE", "Maximum file size is 10 MiB"));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiErrorResponse> handleDatabase(DataAccessException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ApiErrorResponse("DATABASE_UNAVAILABLE", "Database operation failed"));
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    ResponseEntity<ApiErrorResponse> handleAiServiceUnavailable(
        AiServiceUnavailableException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            new ApiErrorResponse("AI_SERVICE_UNAVAILABLE", exception.getMessage())
        );
    }
}
