package com.netflixai.backend;

import org.junit.jupiter.api.Test;

import com.netflixai.backend.entity.Movie;
import com.netflixai.backend.service.TrailerResolutionService;
import com.netflixai.backend.service.TmdbMovieMetadataService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class TrailerResolutionServiceTests {

    private final TrailerResolutionService resolver = new TrailerResolutionService(
            mock(TmdbMovieMetadataService.class));

    @Test
    void resolvesStoredYouTubeUrlToItsOwnMovieAndCachesIt() {
        Movie movie = movie("First Film", "https://youtu.be/abcdefghijk");

        var first = resolver.resolve(movie);
        var second = resolver.resolve(movie);

        assertEquals("abcdefghijk", first.youtubeId());
        assertEquals("https://youtu.be/abcdefghijk", first.trailerUrl());
        assertSame(first, second);
    }

    @Test
    void acceptsAStoredVideoKeyAndBuildsOnlyItsCanonicalYouTubeUrl() {
        var trailer = resolver.resolve(movie("Second Film", "lmnopqrstuv"));

        assertEquals("lmnopqrstuv", trailer.youtubeId());
        assertEquals("https://www.youtube.com/watch?v=lmnopqrstuv", trailer.trailerUrl());
    }

    @Test
    void doesNotInventATrailerWhenTheMovieHasNoVerifiedMapping() {
        assertNull(resolver.resolve(movie("No Trailer", null)));
    }

    @Test
    void rejectsMalformedAndNonYouTubeUrls() {
        assertNull(resolver.resolve(movie("Invalid Key", "https://youtube.com/watch?v=short")));
        assertNull(resolver.resolve(movie("Unrelated URL", "https://example.test/watch?v=abcdefghijk")));
        assertNull(resolver.resolve(movie("Invalid Scheme", "javascript:alert(1)")));
    }

    private Movie movie(String title, String trailerUrl) {
        return new Movie(title, null, null, 2020, null, null, null, null, trailerUrl, null, null, false);
    }
}
