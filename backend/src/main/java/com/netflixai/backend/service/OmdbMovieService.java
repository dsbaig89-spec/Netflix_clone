package com.netflixai.backend.service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflixai.backend.dto.OmdbMovieResponse;
import com.netflixai.backend.dto.OmdbSearchResponse;

import tools.jackson.databind.JsonNode;

@Service
public class OmdbMovieService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OmdbMovieService.class);
    private static final Pattern IMDB_ID = Pattern.compile("^tt\\d{7,10}$");
    private static final Pattern YEAR = Pattern.compile("^(\\d{4})");

    private final RestClient restClient;
    private final String apiKey;
    private final String baseUrl;
    private final AtomicReference<Instant> unavailableUntil = new AtomicReference<>(Instant.EPOCH);

    public OmdbMovieService(@Qualifier("omdbRestClientBuilder") RestClient.Builder restClientBuilder,
                            @Value("${omdb.api-key:}") String apiKey,
                            @Value("${omdb.base-url:https://www.omdbapi.com/}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
    }

    public OmdbSearchResponse search(String query, int page) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }
        if (normalizedQuery.length() > 100) {
            throw new IllegalArgumentException("Search query must be 100 characters or fewer");
        }
        if (page < 1 || page > 100) {
            throw new IllegalArgumentException("OMDb page must be between 1 and 100");
        }

        JsonNode payload = request(normalizedQuery, null, page, null);
        if (!isSuccessful(payload)) {
            String error = payload.path("Error").asString("");
            if (error.toLowerCase().contains("movie not found")) {
                return new OmdbSearchResponse(List.of(), 0, page);
            }
            throw upstreamError();
        }

        List<OmdbMovieResponse> results = new ArrayList<>();
        JsonNode search = payload.path("Search");
        if (search.isArray()) {
            search.forEach(item -> results.add(toMovie(item, false)));
        }
        int totalResults = parseInteger(payload.path("totalResults").asString(null));
        return new OmdbSearchResponse(List.copyOf(results), totalResults, page);
    }

    public OmdbMovieResponse getByImdbId(String imdbId) {
        String normalizedId = imdbId == null ? "" : imdbId.trim();
        if (!IMDB_ID.matcher(normalizedId).matches()) {
            throw new IllegalArgumentException("A valid IMDb title ID is required");
        }

        JsonNode payload = request(null, normalizedId, null, null);
        if (!isSuccessful(payload)) {
            String error = payload.path("Error").asString("");
            if (error.toLowerCase().contains("movie not found")) {
                throw new OmdbApiException(HttpStatus.NOT_FOUND, "MOVIE_NOT_FOUND",
                        "No OMDb movie was found for that IMDb ID");
            }
            throw upstreamError();
        }
        return toMovie(payload, true);
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public OmdbMovieResponse findByTitleAndYear(String title, int year) {
        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty() || normalizedTitle.length() > 100 || year < 1888 || year > 2200) {
            throw new IllegalArgumentException("A movie title and valid release year are required");
        }
        if (Instant.now().isBefore(unavailableUntil.get())) {
            return null;
        }

        JsonNode payload = request(normalizedTitle, null, null, year);
        if (!isSuccessful(payload)) {
            String error = payload.path("Error").asString("");
            if (error.toLowerCase().contains("movie not found")) {
                return null;
            }
            openCircuit();
            throw upstreamError();
        }
        return toMovie(payload, true);
    }

    private JsonNode request(String query, String imdbId, Integer page, Integer year) {
        if (apiKey.isBlank()) {
            throw new OmdbApiException(HttpStatus.SERVICE_UNAVAILABLE, "OMDB_NOT_CONFIGURED",
                    "OMDb search is not configured on the server");
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("apikey", apiKey)
                .queryParam("type", "movie")
                .queryParam("plot", "full");
        if (query != null && year == null) {
            uriBuilder.queryParam("s", query);
            uriBuilder.queryParam("page", page);
        } else if (query != null) {
            uriBuilder.queryParam("t", query);
            uriBuilder.queryParam("y", year);
        }
        if (imdbId != null) {
            uriBuilder.queryParam("i", imdbId);
        }

        URI uri = uriBuilder.build().encode().toUri();
        try {
            return restClient.get().uri(uri).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            LOGGER.warn("OMDb request returned HTTP status {}.", exception.getStatusCode().value());
            openCircuit();
            throw upstreamError();
        } catch (RestClientException exception) {
            LOGGER.warn("OMDb request failed ({}).", exception.getClass().getSimpleName());
            openCircuit();
            throw upstreamError();
        }
    }

    private void openCircuit() {
        unavailableUntil.set(Instant.now().plusSeconds(60));
    }

    private OmdbMovieResponse toMovie(JsonNode item, boolean details) {
        return new OmdbMovieResponse(
                textOrNull(item.path("imdbID").asString(null)),
                textOrNull(item.path("Title").asString(null)),
                parseYear(item.path("Year").asString(null)),
                details ? textOrNull(item.path("Plot").asString(null)) : null,
                parseRating(item.path("imdbRating").asString(null)),
                posterUrl(item.path("Poster").asString(null)),
                details ? parseRuntime(item.path("Runtime").asString(null)) : null,
                details ? parseGenres(item.path("Genre").asString(null)) : List.of()
        );
    }

    private boolean isSuccessful(JsonNode payload) {
        return "true".equalsIgnoreCase(payload.path("Response").asString(""));
    }

    private String posterUrl(String value) {
        String poster = textOrNull(value);
        if (poster == null) {
            return null;
        }
        try {
            URI uri = URI.create(poster);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null ? poster : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String textOrNull(String value) {
        return value == null || value.isBlank() || value.equalsIgnoreCase("N/A") ? null : value.trim();
    }

    private Integer parseYear(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = YEAR.matcher(value.trim());
        return matcher.find() ? parseInteger(matcher.group(1)) : null;
    }

    private Integer parseRuntime(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("^(\\d+)").matcher(value.trim());
        return matcher.find() ? parseInteger(matcher.group(1)) : null;
    }

    private List<String> parseGenres(String value) {
        String genres = textOrNull(value);
        if (genres == null) {
            return List.of();
        }
        return java.util.Arrays.stream(genres.split(","))
                .map(String::trim)
                .filter(genre -> !genre.isEmpty())
                .distinct()
                .toList();
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? 0 : Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private BigDecimal parseRating(String value) {
        try {
            return value == null || value.equalsIgnoreCase("N/A") ? null : new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private OmdbApiException upstreamError() {
        return new OmdbApiException(HttpStatus.BAD_GATEWAY, "OMDB_UNAVAILABLE",
                "OMDb could not complete the movie request. Please try again later.");
    }
}
