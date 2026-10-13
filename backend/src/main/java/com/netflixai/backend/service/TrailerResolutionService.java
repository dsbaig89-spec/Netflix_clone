package com.netflixai.backend.service;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.netflixai.backend.entity.Movie;

@Service
public class TrailerResolutionService {

    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private final TmdbMovieMetadataService tmdbMovieMetadataService;
    private final ConcurrentHashMap<TrailerKey, Optional<Trailer>> cache = new ConcurrentHashMap<>();

    public TrailerResolutionService(TmdbMovieMetadataService tmdbMovieMetadataService) {
        this.tmdbMovieMetadataService = tmdbMovieMetadataService;
    }

    public Trailer resolve(Movie movie) {
        String storedUrl = movie.getTrailerUrl();
        TrailerKey key = new TrailerKey(movie.getId(), storedUrl);
        return cache.computeIfAbsent(key, ignored -> {
                    Trailer storedTrailer = parse(storedUrl);
                    return Optional.ofNullable(storedTrailer == null ? resolveFromTmdb(movie) : storedTrailer);
                })
                .orElse(null);
    }

    private Trailer resolveFromTmdb(Movie movie) {
        TmdbMovieMetadata metadata = tmdbMovieMetadataService.findMetadata(movie);
        if (metadata == null || metadata.youtubeId() == null) {
            return null;
        }
        Trailer trailer = parse(metadata.trailerUrl());
        return trailer != null && trailer.youtubeId().equals(metadata.youtubeId()) ? trailer : null;
    }

    private Trailer parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.trim();
        if (VIDEO_ID.matcher(candidate).matches()) {
            return new Trailer("https://www.youtube.com/watch?v=" + candidate, candidate);
        }

        try {
            URI uri = URI.create(candidate);
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String videoId = "";
            if ("youtu.be".equals(host)) {
                videoId = firstPathSegment(uri);
            } else if ("youtube.com".equals(host) || "www.youtube.com".equals(host)
                    || "m.youtube.com".equals(host) || "youtube-nocookie.com".equals(host)
                    || "www.youtube-nocookie.com".equals(host)) {
                if ("/watch".equals(uri.getPath())) {
                    videoId = queryValue(uri.getRawQuery(), "v");
                } else if (uri.getPath() != null
                        && uri.getPath().matches("^/(embed|shorts|live)/[^/]+/?$")) {
                    videoId = lastPathSegment(uri);
                }
            } else {
                return null;
            }
            return VIDEO_ID.matcher(videoId).matches() ? new Trailer(candidate, videoId) : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String firstPathSegment(URI uri) {
        String path = uri.getPath();
        if (path == null) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1).split("/", 2)[0] : path.split("/", 2)[0];
    }

    private String lastPathSegment(URI uri) {
        String[] segments = uri.getPath().split("/");
        return segments[segments.length - 1];
    }

    private String queryValue(String query, String name) {
        if (query == null) {
            return "";
        }
        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            if (parts.length == 2 && name.equals(parts[0])) {
                return parts[1];
            }
        }
        return "";
    }

    private record TrailerKey(Long movieId, String trailerUrl) {
    }

    public record Trailer(String trailerUrl, String youtubeId) {
    }
}
