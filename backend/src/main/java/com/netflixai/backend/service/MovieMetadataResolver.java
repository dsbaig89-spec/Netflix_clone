package com.netflixai.backend.service;

import java.net.URI;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.netflixai.backend.config.CuratedMoviePosterCatalog;
import com.netflixai.backend.dto.OmdbMovieResponse;
import com.netflixai.backend.entity.Movie;

@Service
public class MovieMetadataResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieMetadataResolver.class);
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{Alnum}]");
    private static final Duration SUCCESS_CACHE_DURATION = Duration.ofHours(6);
    private static final Duration FAILURE_CACHE_DURATION = Duration.ofMinutes(5);

    private final OmdbMovieService omdbMovieService;
    private final TmdbMovieMetadataService tmdbMovieMetadataService;
    private final ConcurrentHashMap<LookupKey, CachedMetadata> cache = new ConcurrentHashMap<>();

    public MovieMetadataResolver(OmdbMovieService omdbMovieService,
                                 TmdbMovieMetadataService tmdbMovieMetadataService) {
        this.omdbMovieService = omdbMovieService;
        this.tmdbMovieMetadataService = tmdbMovieMetadataService;
    }

    public ResolvedMetadata resolve(Movie movie) {
        Integer year = movie.getReleaseYear() != null
                ? movie.getReleaseYear()
                : movie.getReleaseDate() == null ? null : movie.getReleaseDate().getYear();
        String curatedPoster = CuratedMoviePosterCatalog.findPosterUrl(movie.getTitle(), year);
        TmdbMovieMetadata tmdb = null;
        if (!Boolean.TRUE.equals(movie.getDemoContent())
                && tmdbMovieMetadataService.isConfigured()
                && needsTmdbMetadata(movie, curatedPoster)) {
            tmdb = tmdbMovieMetadataService.findMetadata(movie);
        }
        Optional<OmdbMovieResponse> providerMetadata = Optional.empty();

        if (!Boolean.TRUE.equals(movie.getDemoContent())
                && year != null
                && omdbMovieService.isConfigured()
                && needsOmdbMetadata(movie, curatedPoster, tmdb)) {
            LookupKey key = new LookupKey(movie.getId(), normalizeTitle(movie.getTitle()), year);
            CachedMetadata cached = cache.computeIfAbsent(key, ignored -> lookup(movie, year));
            if (cached.expiresAt().isAfter(Instant.now())) {
                providerMetadata = cached.metadata();
            } else {
                cache.remove(key, cached);
                CachedMetadata refreshed = cache.computeIfAbsent(key, ignored -> lookup(movie, year));
                providerMetadata = refreshed.metadata();
            }
        }

        OmdbMovieResponse provider = providerMetadata.orElse(null);
        String storedPoster = validPosterUrl(movie.getPosterUrl());
        String posterUrl = curatedPoster != null
                ? curatedPoster
                : storedPoster != null
                        ? storedPoster
                        : tmdb != null && validPosterUrl(tmdb.posterUrl()) != null
                                ? validPosterUrl(tmdb.posterUrl())
                                : provider == null ? null : validPosterUrl(provider.posterUrl());
        List<String> genres = !movie.getGenres().isEmpty()
                ? movie.getGenres().stream().map(genre -> genre.getName()).sorted().toList()
                : tmdb != null && !tmdb.genres().isEmpty()
                        ? tmdb.genres()
                        : provider == null ? List.of() : provider.genres();

        return new ResolvedMetadata(
                posterUrl,
                movie.getOverview() != null ? movie.getOverview()
                        : tmdb != null && tmdb.overview() != null ? tmdb.overview()
                                : provider == null ? null : provider.plot(),
                year,
                movie.getRuntime() != null ? movie.getRuntime()
                        : tmdb != null && tmdb.runtime() != null ? tmdb.runtime()
                                : provider == null ? null : provider.runtime(),
                movie.getRating() != null ? movie.getRating()
                        : tmdb != null && tmdb.rating() != null ? tmdb.rating()
                                : provider == null ? null : provider.rating(),
                genres
        );
    }

    private boolean needsTmdbMetadata(Movie movie, String curatedPoster) {
        return (curatedPoster == null && validPosterUrl(movie.getPosterUrl()) == null)
                || movie.getOverview() == null
                || movie.getRuntime() == null
                || movie.getRating() == null
                || movie.getGenres().isEmpty()
                || movie.getTrailerUrl() == null;
    }

    private boolean needsOmdbMetadata(Movie movie, String curatedPoster, TmdbMovieMetadata tmdb) {
        return (curatedPoster == null && validPosterUrl(movie.getPosterUrl()) == null
                && (tmdb == null || validPosterUrl(tmdb.posterUrl()) == null))
                || (movie.getOverview() == null && (tmdb == null || tmdb.overview() == null))
                || (movie.getRuntime() == null && (tmdb == null || tmdb.runtime() == null))
                || (movie.getRating() == null && (tmdb == null || tmdb.rating() == null))
                || (movie.getGenres().isEmpty() && (tmdb == null || tmdb.genres().isEmpty()));
    }

    private CachedMetadata lookup(Movie movie, int year) {
        try {
            OmdbMovieResponse result = omdbMovieService.findByTitleAndYear(movie.getTitle(), year);
            if (result == null || !matches(movie.getTitle(), year, result)) {
                return new CachedMetadata(Optional.empty(), Instant.now().plus(FAILURE_CACHE_DURATION));
            }
            return new CachedMetadata(Optional.of(result), Instant.now().plus(SUCCESS_CACHE_DURATION));
        } catch (RuntimeException exception) {
            LOGGER.warn("OMDb metadata lookup failed for movie id {} ({}).",
                    movie.getId(), exception.getClass().getSimpleName());
            return new CachedMetadata(Optional.empty(), Instant.now().plus(FAILURE_CACHE_DURATION));
        }
    }

    private boolean matches(String requestedTitle, int requestedYear, OmdbMovieResponse result) {
        return result.title() != null
                && result.releaseYear() != null
                && result.releaseYear() == requestedYear
                && normalizeTitle(result.title()).equals(normalizeTitle(requestedTitle));
    }

    private String normalizeTitle(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return NON_ALPHANUMERIC.matcher(normalized).replaceAll("");
    }

    private String validPosterUrl(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("N/A")) {
            return null;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !"placehold.co".equalsIgnoreCase(uri.getHost())) {
                return uri.toString();
            }
        } catch (IllegalArgumentException exception) {
            return null;
        }
        return null;
    }

    private record LookupKey(Long movieId, String title, int year) {
    }

    private record CachedMetadata(Optional<OmdbMovieResponse> metadata, Instant expiresAt) {
    }

    public record ResolvedMetadata(
            String posterUrl,
            String overview,
            Integer releaseYear,
            Integer runtime,
            java.math.BigDecimal rating,
            List<String> genres
    ) {
    }
}
