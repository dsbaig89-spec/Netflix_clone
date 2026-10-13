package com.netflixai.backend;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import com.netflixai.backend.dto.OmdbMovieResponse;
import com.netflixai.backend.entity.Movie;
import com.netflixai.backend.service.MovieMetadataResolver;
import com.netflixai.backend.service.OmdbApiException;
import com.netflixai.backend.service.OmdbMovieService;
import com.netflixai.backend.service.TmdbMovieMetadataService;
import com.netflixai.backend.config.CuratedMoviePosterCatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MovieMetadataResolverTests {

    private OmdbMovieService omdbMovieService;
    private TmdbMovieMetadataService tmdbMovieMetadataService;
    private MovieMetadataResolver resolver;

    @BeforeEach
    void setUp() {
        omdbMovieService = mock(OmdbMovieService.class);
        tmdbMovieMetadataService = mock(TmdbMovieMetadataService.class);
        resolver = new MovieMetadataResolver(omdbMovieService, tmdbMovieMetadataService);
    }

    @Test
    void fillsMissingFieldsOnlyFromAnExactTitleAndYearMatchAndCachesIt() {
        when(omdbMovieService.isConfigured()).thenReturn(true);
        when(tmdbMovieMetadataService.isConfigured()).thenReturn(false);
        when(omdbMovieService.findByTitleAndYear("Verified Film", 2021))
                .thenReturn(metadata("Verified Film", 2021, "https://images.example.test/film.jpg"));
        Movie movie = movie("Verified Film", 2021);

        var first = resolver.resolve(movie);
        var second = resolver.resolve(movie);

        assertEquals("https://images.example.test/film.jpg", first.posterUrl());
        assertEquals("A verified plot.", first.overview());
        assertEquals(111, first.runtime());
        assertEquals(new BigDecimal("7.8"), first.rating());
        assertEquals(List.of("Drama", "Thriller"), first.genres());
        assertEquals(first, second);
        verify(omdbMovieService, times(1)).findByTitleAndYear("Verified Film", 2021);
    }

    @Test
    void rejectsSimilarTitleOrWrongYearResults() {
        when(omdbMovieService.isConfigured()).thenReturn(true);
        when(tmdbMovieMetadataService.isConfigured()).thenReturn(false);
        when(omdbMovieService.findByTitleAndYear("Dune", 2021))
                .thenReturn(metadata("Dune: Part Two", 2024, "https://images.example.test/dune2.jpg"));
        when(omdbMovieService.findByTitleAndYear("Dune", 1984))
                .thenReturn(metadata("Dune", 2021, "https://images.example.test/dune.jpg"));

        assertNull(resolver.resolve(movie("Dune", 2021)).posterUrl());
        assertNull(resolver.resolve(movie("Dune", 1984)).posterUrl());
        verify(omdbMovieService).findByTitleAndYear("Dune", 2021);
        verify(omdbMovieService).findByTitleAndYear("Dune", 1984);
    }

    @Test
    void rejectsSameTitleWithWrongReleaseYearAndDoesNotUseItsPoster() {
        when(omdbMovieService.isConfigured()).thenReturn(true);
        when(tmdbMovieMetadataService.isConfigured()).thenReturn(false);
        when(omdbMovieService.findByTitleAndYear("Exact Title", 2020))
                .thenReturn(metadata("Exact Title", 2019, "https://images.example.test/wrong-year.jpg"));

        assertNull(resolver.resolve(movie("Exact Title", 2020)).posterUrl());
    }

    @Test
    void returnsNullPosterWhenNoProviderOrStoredPosterExists() {
        when(omdbMovieService.isConfigured()).thenReturn(false);
        when(tmdbMovieMetadataService.isConfigured()).thenReturn(false);

        var metadata = resolver.resolve(movie("No Poster", 2020));

        assertNull(metadata.posterUrl());
        verify(omdbMovieService, never()).findByTitleAndYear("No Poster", 2020);
    }

    @Test
    void curatedPosterMappingRequiresTheMatchingYearForDuplicateTitles() {
        assertEquals("https://image.tmdb.org/t/p/w500/nrVloCa2hCFOztRF1DZU2jnWIiQ.jpg",
                CuratedMoviePosterCatalog.findPosterUrl("96", 2018));
        assertNull(CuratedMoviePosterCatalog.findPosterUrl("96", 1996));
    }

    @Test
    void ignoresPlaceholderDatabasePostersAndNeverLooksUpFictionalDemoMovies() {
        when(omdbMovieService.isConfigured()).thenReturn(true);
        when(tmdbMovieMetadataService.isConfigured()).thenReturn(false);
        Movie demo = new Movie("Demo Story", "Existing plot", null, 2024, 95, new BigDecimal("7.0"),
                "https://placehold.co/600x900/141414/ffffff?text=Demo", null, null, "English", "US", false);
        demo.setDemoContent(true);

        var metadata = resolver.resolve(demo);

        assertNull(metadata.posterUrl());
        verify(omdbMovieService, never()).findByTitleAndYear("Demo Story", 2024);
    }

    @Test
    void cachesProviderTimeoutAndAuthenticationFailuresWithoutBreakingCatalogueResolution() {
        when(omdbMovieService.isConfigured()).thenReturn(true);
        when(tmdbMovieMetadataService.isConfigured()).thenReturn(false);
        Movie timedOutMovie = movie("Timed Out", 2020);
        when(omdbMovieService.findByTitleAndYear("Timed Out", 2020))
                .thenThrow(new ResourceAccessException("request timed out"));
        Movie unauthorizedMovie = movie("Unauthorized", 2020);
        when(omdbMovieService.findByTitleAndYear("Unauthorized", 2020))
                .thenThrow(new OmdbApiException(HttpStatus.BAD_GATEWAY, "OMDB_UNAVAILABLE", "Provider unavailable"));

        assertNull(resolver.resolve(timedOutMovie).posterUrl());
        assertNull(resolver.resolve(timedOutMovie).posterUrl());
        assertNull(resolver.resolve(unauthorizedMovie).posterUrl());
        assertNull(resolver.resolve(unauthorizedMovie).posterUrl());

        verify(omdbMovieService, times(1)).findByTitleAndYear("Timed Out", 2020);
        verify(omdbMovieService, times(1)).findByTitleAndYear("Unauthorized", 2020);
    }

    private Movie movie(String title, int year) {
        return new Movie(title, null, null, year, null, null, null, null, null, "English", null, false);
    }

    private OmdbMovieResponse metadata(String title, int year, String posterUrl) {
        return new OmdbMovieResponse("tt1234567", title, year, "A verified plot.",
                new BigDecimal("7.8"), posterUrl, 111, List.of("Drama", "Thriller"));
    }
}
