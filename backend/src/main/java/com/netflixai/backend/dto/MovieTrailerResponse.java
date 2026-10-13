package com.netflixai.backend.dto;

public record MovieTrailerResponse(
        Long movieId,
        String title,
        Integer releaseYear,
        String trailerUrl,
        String youtubeId,
        boolean available
) {
}
