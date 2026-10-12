package com.netflixai.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MovieResponse(
        Long id,
        String title,
        String overview,
        LocalDate releaseDate,
        Integer releaseYear,
        Integer runtime,
        BigDecimal rating,
        String posterUrl,
        String backdropUrl,
        String trailerUrl,
        String youtubeId,
        String language,
        String country,
        Boolean featured,
        Boolean demoContent,
        List<String> genres,
        List<CastMemberResponse> cast,
        List<String> directors
) {
    public record CastMemberResponse(String name, String characterName) {
    }
}
