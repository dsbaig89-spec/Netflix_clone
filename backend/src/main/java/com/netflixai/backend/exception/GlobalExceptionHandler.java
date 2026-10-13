package com.netflixai.backend.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.netflixai.backend.service.OmdbApiException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMovieNotFound(MovieNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(404, "MOVIE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception) {
        String message = exception instanceof MethodArgumentNotValidException validationException
                ? validationException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "))
                : exception.getMessage();
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(400, "INVALID_REQUEST", message));
    }

    @ExceptionHandler(OmdbApiException.class)
    public ResponseEntity<ApiErrorResponse> handleOmdbApiError(OmdbApiException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ApiErrorResponse(exception.status().value(), exception.code(), exception.getMessage()));
    }
}
