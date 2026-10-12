package com.netflixai.backend.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import tools.jackson.databind.JsonNode;
import com.netflixai.backend.dto.MovieResponse;
import com.netflixai.backend.entity.Movie;

@Service
public class TmdbMovieMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbMovieMetadataService.class);
    private static final Pattern YOUTUBE_ID = Pattern.compile("^[\\w-]{11}$");
    private static final Duration CACHE_DURATION = Duration.ofHours(6);
    private static final Duration FAILURE_CACHE_DURATION = Duration.ofMinutes(1);
    private static final TmdbMovieMetadata NO_METADATA = new TmdbMovieMetadata(null, null, null,
            List.of(), List.of(), List.of(), null, null);

    private final RestClient restClient;
    private final String apiKey;
    private final String accessToken;
    private final String baseUrl;
    private final ConcurrentMap<String, CachedMetadata> cache = new ConcurrentHashMap<>();

    public TmdbMovieMetadataService(RestClient.Builder restClientBuilder,
                                    @Value("${tmdb.api-key:}") String apiKey,
                                    @Value("${tmdb.access-token:}") String accessToken,
                                    @Value("${tmdb.base-url:https://api.themoviedb.org/3}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.accessToken = accessToken;
        this.baseUrl = baseUrl;
        if (apiKey.isBlank() && accessToken.isBlank()) {
            LOGGER.warn("TMDB credential is not configured; movie metadata and trailer enrichment is disabled.");
        }
    }

    public TmdbMovieMetadata findMetadata(Movie movie) {
        if ((apiKey.isBlank() && accessToken.isBlank()) || movie.getTitle() == null) {
            return null;
        }

        String cacheKey = normalizeTitle(movie.getTitle()) + ":" + movie.getReleaseYear();
        CachedMetadata cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.metadata() == NO_METADATA ? null : cached.metadata();
        }

        try {
            TmdbMovieMetadata metadata = fetchMetadata(movie);
            cache.put(cacheKey, new CachedMetadata(
                    metadata == null ? NO_METADATA : metadata,
                    Instant.now().plus(CACHE_DURATION)));
            return metadata;
        } catch (RestClientException exception) {
            logRequestFailure(movie.getTitle(), exception);
            cache.put(cacheKey, new CachedMetadata(NO_METADATA, Instant.now().plus(FAILURE_CACHE_DURATION)));
            return null;
        }
    }

    private TmdbMovieMetadata fetchMetadata(Movie movie) {
        JsonNode search = get("/search/movie", movie.getTitle(), movie.getReleaseYear(), null);
        JsonNode results = search == null ? null : search.path("results");
        if (results == null || !results.isArray()) {
            return null;
        }

        JsonNode match = findMatchingMovie(results, movie);
        if (match == null || !match.path("id").canConvertToLong()) {
            return null;
        }

        long tmdbMovieId = match.path("id").asLong();
        JsonNode details = get("/movie/" + tmdbMovieId, null, null, "credits");
        if (details == null || details.isMissingNode()) {
            return null;
        }
        JsonNode videos;
        try {
            videos = get("/movie/" + tmdbMovieId + "/videos", null, null, null);
        } catch (RestClientException exception) {
            logRequestFailure(movie.getTitle(), exception);
            videos = null;
        }
        return toMetadata(details, videos);
    }

    private JsonNode get(String path, String query, Integer year, String appendToResponse) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .queryParam("language", "en-US");
        if (accessToken.isBlank()) {
            uriBuilder.queryParam("api_key", apiKey);
        }
        if (query != null) {
            uriBuilder.queryParam("query", query);
        }
        if (year != null) {
            uriBuilder.queryParam("year", year);
        }
        if (appendToResponse != null) {
            uriBuilder.queryParam("append_to_response", appendToResponse);
        }
        URI uri = uriBuilder.build().encode().toUri();
        RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uri);
        if (!accessToken.isBlank()) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        return request.retrieve().body(JsonNode.class);
    }

    private void logRequestFailure(String title, RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            LOGGER.warn("TMDB metadata request failed for '{}' with HTTP status {}.",
                    title, responseException.getStatusCode().value());
        } else {
            LOGGER.warn("TMDB metadata request failed for '{}' ({}).",
                    title, exception.getClass().getSimpleName());
        }
    }

    private JsonNode findMatchingMovie(JsonNode results, Movie movie) {
        String expectedTitle = normalizeTitle(movie.getTitle());
        int expectedYear = movie.getReleaseYear() == null ? 0 : movie.getReleaseYear();
        List<JsonNode> matches = new ArrayList<>();
        results.forEach(result -> {
            String resultTitle = normalizeTitle(result.path("title").asText(""));
            String releaseDate = result.path("release_date").asText("");
            int resultYear = releaseDate.length() >= 4 ? parseYear(releaseDate) : 0;
            if (resultYear == expectedYear && isTitleMatch(expectedTitle, resultTitle)) {
                matches.add(result);
            }
        });
        return matches.stream()
                .sorted(Comparator.comparing(result -> !normalizeTitle(result.path("title").asText(""))
                        .equals(expectedTitle)))
                .findFirst()
                .orElse(null);
    }

    private boolean isTitleMatch(String expectedTitle, String resultTitle) {
        if (expectedTitle.equals(resultTitle)) {
            return true;
        }
        return expectedTitle.equals("asilentvoice")
                && resultTitle.equals("asilentvoicethemovie");
    }

    private int parseYear(String releaseDate) {
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String normalizeTitle(String title) {
        return title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private TmdbMovieMetadata toMetadata(JsonNode details, JsonNode videos) {
        BigDecimal rating = details.path("vote_count").asInt(0) > 0
                ? decimal(details.path("vote_average"))
                : null;
        Integer runtime = positiveInteger(details.path("runtime"));
        String language = languageName(details.path("original_language").asText(""));
        List<String> genres = textValues(details.path("genres"), "name");
        List<String> directors = details.path("credits").path("crew").isArray()
                ? valuesWhere(details.path("credits").path("crew"), "job", "Director", "name")
                : List.of();
        List<MovieResponse.CastMemberResponse> cast = castMembers(details.path("credits").path("cast"));
        String youtubeId = trailerId(videos == null ? null : videos.path("results"));
        String trailerUrl = youtubeId == null ? null : "https://www.youtube.com/watch?v=" + youtubeId;

        if (rating == null && runtime == null && language == null && genres.isEmpty()
                && directors.isEmpty() && cast.isEmpty() && youtubeId == null) {
            return null;
        }
        return new TmdbMovieMetadata(rating, runtime, language, genres, directors, cast, trailerUrl, youtubeId);
    }

    private BigDecimal decimal(JsonNode value) {
        if (!value.isNumber()) {
            return null;
        }
        BigDecimal number = value.decimalValue();
        return number.signum() > 0 ? number : null;
    }

    private Integer positiveInteger(JsonNode value) {
        return value.canConvertToInt() && value.asInt() > 0 ? value.asInt() : null;
    }

    private String languageName(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String name = Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH);
        return name.isBlank() || name.equalsIgnoreCase(code) ? null : name;
    }

    private List<String> textValues(JsonNode values, String field) {
        if (!values.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        values.forEach(value -> {
            String text = value.path(field).asText("").trim();
            if (!text.isEmpty()) {
                result.add(text);
            }
        });
        return List.copyOf(result);
    }

    private List<String> valuesWhere(JsonNode values, String filterField, String filterValue, String valueField) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> {
            if (filterValue.equalsIgnoreCase(value.path(filterField).asText(""))) {
                String text = value.path(valueField).asText("").trim();
                if (!text.isEmpty() && !result.contains(text)) {
                    result.add(text);
                }
            }
        });
        return List.copyOf(result);
    }

    private List<MovieResponse.CastMemberResponse> castMembers(JsonNode values) {
        if (!values.isArray()) {
            return List.of();
        }
        List<MovieResponse.CastMemberResponse> result = new ArrayList<>();
        for (JsonNode value : values) {
            String name = value.path("name").asText("").trim();
            if (!name.isEmpty()) {
                String character = value.path("character").asText("").trim();
                result.add(new MovieResponse.CastMemberResponse(name, character.isEmpty() ? null : character));
            }
            if (result.size() == 10) {
                break;
            }
        }
        return List.copyOf(result);
    }

    private String trailerId(JsonNode videos) {
        if (!videos.isArray()) {
            return null;
        }
        return java.util.stream.StreamSupport.stream(videos.spliterator(), false)
                .filter(video -> "YouTube".equalsIgnoreCase(video.path("site").asText(""))
                        && "Trailer".equalsIgnoreCase(video.path("type").asText(""))
                        && YOUTUBE_ID.matcher(video.path("key").asText("")).matches())
                .sorted(Comparator.comparing((JsonNode video) -> !video.path("official").asBoolean(false)))
                .map(video -> video.path("key").asText())
                .findFirst()
                .orElse(null);
    }

    private record CachedMetadata(TmdbMovieMetadata metadata, Instant expiresAt) {
    }
}
