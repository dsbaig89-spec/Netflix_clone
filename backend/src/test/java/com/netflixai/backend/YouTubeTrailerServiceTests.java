package com.netflixai.backend;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.netflixai.backend.entity.Movie;
import com.netflixai.backend.service.MovieService;
import com.netflixai.backend.service.TrailerResolutionService;
import com.netflixai.backend.service.YouTubeTrailerService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YouTubeTrailerServiceTests {

    private static final String BASE_URL = "https://youtube.test/youtube/v3";

    private MockRestServiceServer server;
    private MovieService movieService;
    private TrailerResolutionService storedTrailerResolver;
    private YouTubeTrailerService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        movieService = mock(MovieService.class);
        storedTrailerResolver = mock(TrailerResolutionService.class);
        service = new YouTubeTrailerService(builder, "test-key", BASE_URL,
                movieService, storedTrailerResolver);
    }

    @Test
    void searchesEmbeddableVideosAndSelectsTheBestTitleYearTrailerRatherThanTheFirstResult() {
        Movie movie = movie(7L, "The Matrix", 1999, null);
        when(movieService.getById(7L)).thenReturn(movie);
        when(storedTrailerResolver.resolve(movie)).thenReturn(null);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
                .andExpect(queryParam("videoEmbeddable", "true"))
                .andExpect(queryParam("type", "video"))
                .andExpect(queryParam("q", "The%20Matrix%201999%20official%20trailer"))
                .andRespond(withSuccess("""
                        {"items":[
                          {"id":{"videoId":"zzzzzzzzzzz"},"snippet":{"title":"The Matrix Reloaded Official Trailer","description":"Trailer","channelTitle":"Studio"}},
                          {"id":{"videoId":"aaaaaaaaaaa"},"snippet":{"title":"The Matrix Teaser","description":"Teaser","channelTitle":"Fan Channel"}},
                          {"id":{"videoId":"bbbbbbbbbbb"},"snippet":{"title":"The Matrix Official Trailer (1999)","description":"Official trailer","channelTitle":"Studio Official"}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        var trailer = service.resolve(7L);

        assertTrue(trailer.available());
        assertEquals("bbbbbbbbbbb", trailer.youtubeId());
        assertEquals("https://www.youtube.com/watch?v=bbbbbbbbbbb", trailer.trailerUrl());
        assertEquals(1999, trailer.releaseYear());
        server.verify();
    }

    @Test
    void cachesSuccessfulTrailerAndNeverReusesItForAnUnrelatedMovie() {
        Movie first = movie(1L, "First Film", 2020, null);
        Movie second = movie(2L, "Second Film", 2021, null);
        when(movieService.getById(1L)).thenReturn(first);
        when(movieService.getById(2L)).thenReturn(second);
        when(storedTrailerResolver.resolve(first)).thenReturn(null);
        when(storedTrailerResolver.resolve(second)).thenReturn(null);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
                .andRespond(withSuccess("""
                        {"items":[{"id":{"videoId":"ccccccccccc"},
                          "snippet":{"title":"First Film Official Trailer","description":"Trailer","channelTitle":"Studio"}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
                .andRespond(withSuccess("""
                        {"items":[{"id":{"videoId":"ccccccccccc"},
                          "snippet":{"title":"Second Film Official Trailer","description":"Trailer","channelTitle":"Studio"}}]}
                        """, MediaType.APPLICATION_JSON));

        var firstResult = service.resolve(1L);
        var cachedResult = service.resolve(1L);
        var secondResult = service.resolve(2L);

        assertEquals("ccccccccccc", firstResult.youtubeId());
        assertEquals(firstResult, cachedResult);
        assertFalse(secondResult.available());
        assertNull(secondResult.youtubeId());
        server.verify();
    }

    @Test
    void keepsAValidStoredTrailerWhenYouTubeCredentialsAreMissing() {
        Movie movie = movie(3L, "Stored Film", 2020, "https://youtu.be/ddddddddddd");
        when(movieService.getById(3L)).thenReturn(movie);
        when(storedTrailerResolver.resolve(movie))
                .thenReturn(new TrailerResolutionService.Trailer(movie.getTrailerUrl(), "ddddddddddd"));
        YouTubeTrailerService unconfigured = new YouTubeTrailerService(
                RestClient.builder(), "", BASE_URL, movieService, storedTrailerResolver);

        var trailer = unconfigured.resolve(3L);

        assertTrue(trailer.available());
        assertEquals("ddddddddddd", trailer.youtubeId());
        server.verify();
    }

    @Test
    void returnsUnavailableForMissingCredentialsAndCachesMissingResults() {
        Movie movie = movie(4L, "No Trailer", 2020, null);
        when(movieService.getById(4L)).thenReturn(movie);
        when(storedTrailerResolver.resolve(movie)).thenReturn(null);
        YouTubeTrailerService unconfigured = new YouTubeTrailerService(
                RestClient.builder(), "", BASE_URL, movieService, storedTrailerResolver);

        assertFalse(unconfigured.resolve(4L).available());
        assertFalse(unconfigured.resolve(4L).available());
        server.verify();
    }

    @Test
    void gracefullyHandlesInvalidCredentialsAndQuotaExhaustion() {
        Movie movie = movie(5L, "Provider Failure", 2020, null);
        when(movieService.getById(5L)).thenReturn(movie);
        when(storedTrailerResolver.resolve(movie)).thenReturn(null);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        var trailer = service.resolve(5L);

        assertFalse(trailer.available());
        assertNull(trailer.youtubeId());
        assertFalse(service.resolve(5L).available());
        server.verify();
    }

    @Test
    void acceptsExactMovieTitleWithStudioAndCastTextAfterTheTrailerTitle() {
        Movie movie = movie(54L, "777 Charlie", 2022, null);
        when(movieService.getById(54L)).thenReturn(movie);
        when(storedTrailerResolver.resolve(movie)).thenReturn(null);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
                .andRespond(withSuccess("""
                        {"items":[{"id":{"videoId":"Z3r7f9zw-mo"},
                          "snippet":{"title":"777 Charlie Official Trailer | Paramvah Pictures",
                          "description":"Official trailer","channelTitle":"Paramvah Pictures"}}]}
                        """, MediaType.APPLICATION_JSON));

        var trailer = service.resolve(54L);

        assertTrue(trailer.available());
        assertEquals("Z3r7f9zw-mo", trailer.youtubeId());
        server.verify();
    }

    @Test
    void rejectsCandidateTitlesWithAnotherReleaseYearAndDoesNotChooseUnrelatedClips() {
        Movie movie = movie(6L, "Quiet Frequency", 2024, null);
        when(movieService.getById(6L)).thenReturn(movie);
        when(storedTrailerResolver.resolve(movie)).thenReturn(null);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
                .andRespond(withSuccess("""
                        {"items":[
                          {"id":{"videoId":"eeeeeeeeeee"},"snippet":{"title":"Quiet Frequency Official Trailer (2023)","description":"Trailer","channelTitle":"Studio"}},
                          {"id":{"videoId":"fffffffffff"},"snippet":{"title":"Quiet Frequency Interview","description":"Trailer","channelTitle":"Studio"}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        var trailer = service.resolve(6L);

        assertFalse(trailer.available());
        assertNull(trailer.youtubeId());
        server.verify();
    }

    private Movie movie(Long id, String title, int year, String trailerUrl) {
        Movie movie = new Movie(title, "Existing synopsis.", LocalDate.of(year, 1, 1),
                year, null, null, null, null, trailerUrl, null, null, false);
        try {
            var idField = Movie.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(movie, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to initialize movie test ID", exception);
        }
        return movie;
    }
}
