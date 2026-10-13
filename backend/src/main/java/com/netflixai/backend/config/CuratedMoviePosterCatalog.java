package com.netflixai.backend.config;

import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class CuratedMoviePosterCatalog {

    private static final String TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{Alnum}]");
    private static final Map<MovieIdentity, String> POSTERS = Map.of(
            new MovieIdentity("3 Idiots", 2009), TMDB_POSTER_BASE_URL + "/66A9MqXOyVFCssoloscw79z8Tew.jpg",
            new MovieIdentity("777 Charlie", 2022), TMDB_POSTER_BASE_URL + "/qArPmXH0aWsT3SEtYl8XrU2Oz48.jpg",
            new MovieIdentity("96", 2018), TMDB_POSTER_BASE_URL + "/nrVloCa2hCFOztRF1DZU2jnWIiQ.jpg",
            new MovieIdentity("A Silent Voice", 2016), TMDB_POSTER_BASE_URL + "/tuFaWiqX0TXoWu7DGNcmX3UW7sT.jpg",
            new MovieIdentity("Inception", 2010), TMDB_POSTER_BASE_URL + "/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
            new MovieIdentity("Interstellar", 2014), TMDB_POSTER_BASE_URL + "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
            new MovieIdentity("The Dark Knight", 2008), TMDB_POSTER_BASE_URL + "/qJ2tW6WMUDux911r6m7haRef0WH.jpg"
    );

    private CuratedMoviePosterCatalog() {
    }

    public static String findPosterUrl(String title, Integer releaseYear) {
        if (title == null || title.isBlank() || releaseYear == null) {
            return null;
        }
        String normalizedTitle = normalizeTitle(title);
        return POSTERS.entrySet().stream()
                .filter(entry -> entry.getKey().releaseYear() == releaseYear
                        && normalizeTitle(entry.getKey().title()).equals(normalizedTitle))
                .map(Map.Entry::getValue)
                .filter(CuratedMoviePosterCatalog::isHttpUrl)
                .findFirst()
                .orElse(null);
    }

    private static String normalizeTitle(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return NON_ALPHANUMERIC.matcher(normalized).replaceAll("");
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && uri.getPath() != null
                    && uri.getPath().toLowerCase().matches(".*\\.(jpg|jpeg|png|webp)$");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private record MovieIdentity(String title, int releaseYear) {
    }
}
