package com.netflixai.backend.service;

import java.math.BigDecimal;
import java.net.URI;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflixai.backend.dto.MovieResponse;
import com.netflixai.backend.entity.Movie;

import tools.jackson.databind.JsonNode;

@Service
public class TmdbMovieMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbMovieMetadataService.class);
    private static final Pattern YOUTUBE_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final Pattern POSTER_PATH = Pattern.compile("^/[A-Za-z0-9_./-]+\\.(jpg|jpeg|png|webp)$");
    private static final Duration SUCCESS_CACHE_DURATION = Duration.ofHours(6);
    private static final Duration FAILURE_CACHE_DURATION = Duration.ofMinutes(5);
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    private final RestClient restClient;
    private final String apiKey;
    private final String accessToken;
    private final String baseUrl;
    private final ConcurrentHashMap<LookupKey, CachedMetadata> cache = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> unavailableUntil = new AtomicReference<>(Instant.EPOCH);

    public TmdbMovieMetadataService(
            @Qualifier("tmdbRestClientBuilder") RestClient.Builder restClientBuilder,
            @Value("${tmdb.api-key:}") String apiKey,
            @Value("${tmdb.access-token:}") String accessToken,
            @Value("${tmdb.base-url:https://api.themoviedb.org/3}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() || !accessToken.isBlank();
    }

    public TmdbMovieMetadata findMetadata(Movie movie) {
        Integer year = movie.getReleaseYear() != null
                ? movie.getReleaseYear()
                : movie.getReleaseDate() == null ? null : movie.getReleaseDate().getYear();
        if (!isConfigured() || movie.getTitle() == null || movie.getTitle().isBlank() || year == null) {
            return null;
        }
        if (Instant.now().isBefore(unavailableUntil.get())) {
            return null;
        }

        LookupKey key = new LookupKey(movie.getId(), normalizeTitle(movie.getTitle()), year);
        CachedMetadata cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.metadata();
        }

        try {
            TmdbMovieMetadata metadata = fetchMetadata(movie, year);
            if (metadata != null) {
                cache.put(key, new CachedMetadata(metadata, Instant.now().plus(SUCCESS_CACHE_DURATION)));
            } else {
                cache.put(key, new CachedMetadata(null, Instant.now().plus(FAILURE_CACHE_DURATION)));
            }
            return metadata;
        } catch (RestClientException exception) {
            logRequestFailure(movie.getId(), exception);
            unavailableUntil.set(Instant.now().plus(FAILURE_CACHE_DURATION));
            cache.put(key, new CachedMetadata(null, Instant.now().plus(FAILURE_CACHE_DURATION)));
            return null;
        }
    }

    private TmdbMovieMetadata fetchMetadata(Movie movie, int year) {
        JsonNode search = get("/search/movie", movie.getTitle(), year, null);
        JsonNode results = search == null ? null : search.path("results");
        JsonNode match = findMatchingMovie(results, movie.getTitle(), year);
        if (match == null || !match.path("id").canConvertToLong()) {
            return null;
        }

        long tmdbMovieId = match.path("id").asLong();
        JsonNode details = get("/movie/" + tmdbMovieId, null, null, "credits");
        if (details == null || details.isMissingNode()) {
            return null;
        }
        JsonNode videos = get("/movie/" + tmdbMovieId + "/videos", null, null, null);
        return toMetadata(details, videos);
    }

    private JsonNode get(String path, String query, Integer year, String appendToResponse) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .queryParam("language", "en-US");
        if (accessToken.isBlank()) {
            builder.queryParam("api_key", apiKey);
        }
        if (query != null) {
            builder.queryParam("query", query);
        }
        if (year != null) {
            builder.queryParam("year", year);
        }
        if (appendToResponse != null) {
            builder.queryParam("append_to_response", appendToResponse);
        }

        URI uri = builder.build().encode().toUri();
        RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uri);
        if (!accessToken.isBlank()) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        return request.retrieve().body(JsonNode.class);
    }

    private JsonNode findMatchingMovie(JsonNode results, String expectedTitle, int expectedYear) {
        if (results == null || !results.isArray()) {
            return null;
        }
        List<JsonNode> matches = new ArrayList<>();
        results.forEach(result -> {
            String releaseDate = result.path("release_date").asString("");
            String resultTitle = result.path("title").asString("");
            if (yearFromDate(releaseDate) == expectedYear
                    && normalizeTitle(resultTitle).equals(normalizeTitle(expectedTitle))) {
                matches.add(result);
            }
        });
        return matches.stream().findFirst().orElse(null);
    }

    private int yearFromDate(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String normalizeTitle(String title) {
        return Normalizer.normalize(title, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private TmdbMovieMetadata toMetadata(JsonNode details, JsonNode videos) {
        BigDecimal rating = details.path("vote_count").asInt(0) > 0
                && details.path("vote_average").isNumber()
                ? details.path("vote_average").decimalValue() : null;
        Integer runtime = positiveInteger(details.path("runtime"));
        String language = languageName(details.path("original_language").asString(""));
        List<String> genres = textValues(details.path("genres"), "name");
        List<String> directors = valuesWhere(details.path("credits").path("crew"), "job", "Director", "name");
        List<MovieResponse.CastMemberResponse> cast = castMembers(details.path("credits").path("cast"));
        String posterUrl = posterUrl(details.path("poster_path").asString(null));
        String overview = textOrNull(details.path("overview").asString(null));
        String youtubeId = trailerId(videos == null ? null : videos.path("results"));
        String trailerUrl = youtubeId == null ? null : "https://www.youtube.com/watch?v=" + youtubeId;

        if (rating == null && runtime == null && language == null && genres.isEmpty()
                && directors.isEmpty() && cast.isEmpty() && posterUrl == null
                && overview == null && youtubeId == null) {
            return null;
        }
        return new TmdbMovieMetadata(rating, runtime, language, genres, directors, cast,
                posterUrl, overview, trailerUrl, youtubeId);
    }

    private String posterUrl(String posterPath) {
        if (posterPath == null || !POSTER_PATH.matcher(posterPath).matches()
                || posterPath.contains("..")) {
            return null;
        }
        return IMAGE_BASE_URL + posterPath;
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
            String text = textOrNull(value.path(field).asString(null));
            if (text != null && !result.contains(text)) {
                result.add(text);
            }
        });
        return List.copyOf(result);
    }

    private List<String> valuesWhere(JsonNode values, String filterField, String filterValue, String valueField) {
        if (!values.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        values.forEach(value -> {
            if (filterValue.equalsIgnoreCase(value.path(filterField).asString(""))) {
                String text = textOrNull(value.path(valueField).asString(null));
                if (text != null && !result.contains(text)) {
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
            String name = textOrNull(value.path("name").asString(null));
            if (name != null) {
                result.add(new MovieResponse.CastMemberResponse(name,
                        textOrNull(value.path("character").asString(null))));
            }
            if (result.size() == 10) {
                break;
            }
        }
        return List.copyOf(result);
    }

    private String trailerId(JsonNode videos) {
        if (videos == null || !videos.isArray()) {
            return null;
        }
        return StreamSupport.stream(videos.spliterator(), false)
                .filter(video -> "YouTube".equalsIgnoreCase(video.path("site").asString(""))
                        && "Trailer".equalsIgnoreCase(video.path("type").asString(""))
                        && YOUTUBE_ID.matcher(video.path("key").asString("")).matches())
                .sorted(Comparator.comparing((JsonNode video) -> !video.path("official").asBoolean(false)))
                .map(video -> video.path("key").asString())
                .findFirst()
                .orElse(null);
    }

    private String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void logRequestFailure(Long movieId, RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            LOGGER.warn("TMDB metadata request failed for movie id {} with HTTP status {}.",
                    movieId, responseException.getStatusCode().value());
        } else {
            LOGGER.warn("TMDB metadata request failed for movie id {} ({}).",
                    movieId, exception.getClass().getSimpleName());
        }
    }

    private record LookupKey(Long movieId, String title, int year) {
    }

    private record CachedMetadata(TmdbMovieMetadata metadata, Instant expiresAt) {
    }
}
