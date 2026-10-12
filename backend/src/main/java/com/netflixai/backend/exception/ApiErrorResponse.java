package com.netflixai.backend.exception;

public record ApiErrorResponse(int status, String error, String message) {
}
