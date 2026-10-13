package com.netflixai.backend.dto;

import java.util.List;

public record OmdbSearchResponse(List<OmdbMovieResponse> results, int totalResults, int page) {
}
