package com.netflixai.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MovieSummaryResponse(
        Long id,
        String title,
        String overview,
        LocalDate releaseDate,
        Integer releaseYear,
        Integer runtime,
        BigDecimal rating,
        String posterUrl,
        String backdropUrl,
        String language,
        String country,
        Boolean featured,
        List<String> genres,
        String trailerUrl,
        String youtubeId
) {
}
