package com.netflixai.backend.service;

import java.math.BigDecimal;
import java.util.List;

import com.netflixai.backend.dto.MovieResponse;

public record TmdbMovieMetadata(
        BigDecimal rating,
        Integer runtime,
        String language,
        List<String> genres,
        List<String> directors,
        List<MovieResponse.CastMemberResponse> cast,
        String posterUrl,
        String overview,
        String trailerUrl,
        String youtubeId
) {
}
