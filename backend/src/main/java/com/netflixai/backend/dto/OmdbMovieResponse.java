package com.netflixai.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record OmdbMovieResponse(
        String imdbId,
        String title,
        Integer releaseYear,
        String plot,
        BigDecimal rating,
        String posterUrl,
        Integer runtime,
        List<String> genres
) {
}
