package com.netflixai.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MovieCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String overview,
        LocalDate releaseDate,
        Integer releaseYear,
        Integer runtime,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal rating,
        @Size(max = 500) String posterUrl,
        @Size(max = 500) String backdropUrl,
        @Size(max = 500) String trailerUrl,
        @Size(max = 20) String language,
        @Size(max = 100) String country,
        Boolean featured,
        Set<@NotBlank @Size(max = 80) String> genres,
        Set<@NotBlank @Size(max = 150) String> cast,
        Set<@NotBlank @Size(max = 150) String> directors
) {
}
