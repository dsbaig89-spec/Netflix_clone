package com.netflixai.backend.service;

import java.net.URI;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflixai.backend.dto.MovieTrailerResponse;
import com.netflixai.backend.entity.Movie;

import tools.jackson.databind.JsonNode;

@Service
public class YouTubeTrailerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeTrailerService.class);
    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final Pattern YEAR = Pattern.compile("\\b(?:19|20)\\d{2}\\b");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern API_ERROR_REASON = Pattern.compile("\"reason\"\\s*:\\s*\"([A-Za-z0-9_.-]+)\"");
    private static final Pattern API_ERROR_STATUS = Pattern.compile("\"status\"\\s*:\\s*\"([A-Za-z0-9_.-]+)\"");
    private static final Duration NOT_FOUND_CACHE_DURATION = Duration.ofMinutes(10);
    private static final Duration FAILURE_BACKOFF = Duration.ofMinutes(2);
    private static final List<String> TRAILER_WORDS = List.of("trailer", "teaser");
    private static final List<String> ALLOWED_TITLE_SUFFIXES = List.of(
            "official", "trailer", "teaser", "final", "international", "theatrical",
            "red", "band", "green", "band", "hd", "4k", "1080p", "2160p",
            "movie", "film", "english", "subtitles", "subtitle", "sub", "dub", "full");

    private final RestClient restClient;
    private final String apiKey;
    private final String baseUrl;
    private final MovieService movieService;
    private final TrailerResolutionService storedTrailerResolver;
    private final ConcurrentHashMap<MovieKey, CachedTrailer> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MovieIdentity> videoOwners = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> providerUnavailableUntil = new AtomicReference<>(Instant.EPOCH);

    public YouTubeTrailerService(
            @Qualifier("youtubeRestClientBuilder") RestClient.Builder restClientBuilder,
            @Value("${youtube.api-key:}") String apiKey,
            @Value("${youtube.base-url:https://www.googleapis.com/youtube/v3}") String baseUrl,
            MovieService movieService,
            TrailerResolutionService storedTrailerResolver) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
        this.movieService = movieService;
        this.storedTrailerResolver = storedTrailerResolver;
        LOGGER.info("YouTube Data API key configured: {}", !this.apiKey.isBlank());
    }

    public MovieTrailerResponse resolve(Long movieId) {
        Movie movie = movieService.getById(movieId);
        Integer year = releaseYear(movie);
        MovieTrailerResponse stored = resolveStoredTrailer(movie, year);
        if (stored != null) {
            return stored;
        }

        if (year == null || movie.getTitle() == null || movie.getTitle().isBlank()) {
            return unavailable(movie, year);
        }

        MovieKey key = new MovieKey(movie.getId(), normalize(movie.getTitle()), year);
        CachedTrailer cached = cache.get(key);
        if (cached != null && (cached.trailer() != null || cached.expiresAt().isAfter(Instant.now()))) {
            return cached.trailer() == null
                    ? unavailable(movie, year)
                    : response(movie, year, cached.trailer());
        }
        if (cached != null) {
            cache.remove(key, cached);
        }

        if (apiKey.isBlank() || Instant.now().isBefore(providerUnavailableUntil.get())) {
            if (apiKey.isBlank()) {
                LOGGER.warn("YouTube trailer lookup skipped for '{}' (movie id {}): API key is not configured.",
                        movie.getTitle(), movie.getId());
            } else {
                LOGGER.info("YouTube trailer lookup skipped for '{}' (movie id {}): provider backoff is active.",
                        movie.getTitle(), movie.getId());
            }
            return unavailable(movie, year);
        }

        MovieTrailerResponse resolved = search(movie, year, key);
        return resolved == null ? unavailable(movie, year) : resolved;
    }

    private MovieTrailerResponse resolveStoredTrailer(Movie movie, Integer year) {
        TrailerResolutionService.Trailer existing = storedTrailerResolver.resolve(movie);
        if (existing == null || !VIDEO_ID.matcher(existing.youtubeId()).matches()) {
            return null;
        }
        MovieIdentity owner = new MovieIdentity(normalize(movie.getTitle()), year);
        MovieIdentity previousOwner = videoOwners.putIfAbsent(existing.youtubeId(), owner);
        if (previousOwner != null && !previousOwner.equals(owner)) {
            LOGGER.warn("Ignoring a stored YouTube video reused by unrelated movie id {}.", movie.getId());
            return null;
        }
        return response(movie, year, existing);
    }

    private MovieTrailerResponse search(Movie movie, int year, MovieKey key) {
        try {
            JsonNode payload = restClient.get()
                    .uri(UriComponentsBuilder.fromUriString(baseUrl)
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("type", "video")
                            .queryParam("videoEmbeddable", "true")
                            .queryParam("maxResults", 10)
                            .queryParam("q", movie.getTitle() + " " + year + " official trailer")
                            .queryParam("key", apiKey)
                            .build()
                            .encode()
                            .toUri())
                    .retrieve()
                    .body(JsonNode.class);

            int resultCount = payload != null && payload.path("items").isArray()
                    ? payload.path("items").size() : 0;
            LOGGER.info("YouTube trailer search completed for '{}' (movie id {}): HTTP 200, {} results.",
                    movie.getTitle(), movie.getId(), resultCount);
            TrailerResolutionService.Trailer selected = selectCandidate(payload, movie.getTitle(), year, key);
            if (selected == null) {
                LOGGER.info("No title/year-compatible embeddable trailer selected for '{}' (movie id {}).",
                        movie.getTitle(), movie.getId());
                cache.put(key, new CachedTrailer(null, Instant.now().plus(NOT_FOUND_CACHE_DURATION)));
                return unavailable(movie, year);
            }
            LOGGER.info("Selected verified trailer for '{}' (movie id {}).", movie.getTitle(), movie.getId());
            cache.put(key, new CachedTrailer(selected, Instant.MAX));
            return response(movie, year, selected);
        } catch (RestClientResponseException exception) {
            LOGGER.warn("YouTube trailer lookup failed for '{}' (movie id {}): HTTP {}, API error code {}.",
                    movie.getTitle(), movie.getId(), exception.getStatusCode().value(),
                    apiErrorCode(exception.getResponseBodyAsString()));
            providerUnavailableUntil.set(Instant.now().plus(FAILURE_BACKOFF));
            cache.put(key, new CachedTrailer(null, Instant.now().plus(FAILURE_BACKOFF)));
            return unavailable(movie, year);
        } catch (RestClientException exception) {
            LOGGER.warn("YouTube trailer lookup failed for '{}' (movie id {}, {}).",
                    movie.getTitle(), movie.getId(), exception.getClass().getSimpleName());
            providerUnavailableUntil.set(Instant.now().plus(FAILURE_BACKOFF));
            cache.put(key, new CachedTrailer(null, Instant.now().plus(FAILURE_BACKOFF)));
            return unavailable(movie, year);
        }
    }

    private TrailerResolutionService.Trailer selectCandidate(
            JsonNode payload, String movieTitle, int year, MovieKey movieKey) {
        if (payload == null || !payload.path("items").isArray()) {
            return null;
        }
        List<Candidate> candidates = new ArrayList<>();
        payload.path("items").forEach(item -> {
            String videoId = item.path("id").path("videoId").asString("");
            JsonNode snippet = item.path("snippet");
            String title = snippet.path("title").asString("");
            if (!VIDEO_ID.matcher(videoId).matches()
                    || !isMovieTrailerTitle(title, movieTitle, year)
                    || !hasTrailerContext(title)) {
                return;
            }

            MovieIdentity owner = new MovieIdentity(normalize(movieTitle), year);
            MovieIdentity previousOwner = videoOwners.get(videoId);
            if (previousOwner != null && !previousOwner.equals(owner)) {
                return;
            }
            int score = scoreCandidate(title, snippet.path("channelTitle").asString(""));
            candidates.add(new Candidate(videoId, score));
        });

        candidates.sort((left, right) -> Integer.compare(right.score(), left.score()));
        for (Candidate candidate : candidates) {
            MovieIdentity owner = new MovieIdentity(movieKey.title(), movieKey.year());
            MovieIdentity previousOwner = videoOwners.putIfAbsent(candidate.videoId(), owner);
            if (previousOwner == null || previousOwner.equals(owner)) {
                return new TrailerResolutionService.Trailer(
                        "https://www.youtube.com/watch?v=" + candidate.videoId(), candidate.videoId());
            }
        }
        return null;
    }

    private boolean isMovieTrailerTitle(String candidateTitle, String movieTitle, int releaseYear) {
        List<String> titleTokens = tokens(movieTitle);
        List<String> candidateTokens = tokens(candidateTitle);
        if (titleTokens.isEmpty() || candidateTokens.size() < titleTokens.size()) {
            return false;
        }
        for (int index = 0; index < titleTokens.size(); index++) {
            if (!titleTokens.get(index).equals(candidateTokens.get(index))) {
                return false;
            }
        }

        for (int index = titleTokens.size(); index < candidateTokens.size(); index++) {
            String suffix = candidateTokens.get(index);
            if (YEAR.matcher(suffix).matches()) {
                if (Integer.parseInt(suffix) != releaseYear) {
                    return false;
                }
            } else if (isSequelMarker(candidateTokens, index)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSequelMarker(List<String> candidateTokens, int index) {
        String token = candidateTokens.get(index);
        if (List.of("part", "chapter", "volume", "vol", "episode").contains(token)) {
            return true;
        }
        return token.matches("\\d{1,2}")
                && index > 0
                && !ALLOWED_TITLE_SUFFIXES.contains(candidateTokens.get(index - 1));
    }

    private String apiErrorCode(String responseBody) {
        Matcher reason = API_ERROR_REASON.matcher(responseBody == null ? "" : responseBody);
        if (reason.find()) {
            return reason.group(1);
        }
        Matcher status = API_ERROR_STATUS.matcher(responseBody == null ? "" : responseBody);
        return status.find() ? status.group(1) : "not-provided";
    }

    private boolean hasTrailerContext(String title) {
        String normalizedTitle = normalize(title);
        return TRAILER_WORDS.stream().anyMatch(normalizedTitle::contains);
    }

    private int scoreCandidate(String title, String channelTitle) {
        String normalizedTitle = normalize(title);
        String normalizedChannel = normalize(channelTitle);
        int score = normalizedTitle.contains("official") ? 100 : 0;
        score += normalizedTitle.contains("trailer") ? 50 : 0;
        score += normalizedTitle.contains("teaser") ? 25 : 0;
        score += normalizedChannel.contains("official") ? 20 : 0;
        return score;
    }

    private List<String> tokens(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return List.of(NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ").trim().split("\\s+"));
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return NON_ALPHANUMERIC.matcher(normalized).replaceAll("");
    }

    private Integer releaseYear(Movie movie) {
        return movie.getReleaseYear() != null
                ? movie.getReleaseYear()
                : movie.getReleaseDate() == null ? null : movie.getReleaseDate().getYear();
    }

    private MovieTrailerResponse response(
            Movie movie, Integer year, TrailerResolutionService.Trailer trailer) {
        return new MovieTrailerResponse(movie.getId(), movie.getTitle(), year,
                trailer.trailerUrl(), trailer.youtubeId(), true);
    }

    private MovieTrailerResponse unavailable(Movie movie, Integer year) {
        return new MovieTrailerResponse(movie.getId(), movie.getTitle(), year, null, null, false);
    }

    private record MovieKey(Long id, String title, int year) {
    }

    private record MovieIdentity(String title, Integer year) {
    }

    private record CachedTrailer(TrailerResolutionService.Trailer trailer, Instant expiresAt) {
    }

    private record Candidate(String videoId, int score) {
    }
}
