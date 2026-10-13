package com.netflixai.backend;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.netflixai.backend.dto.OmdbMovieResponse;
import com.netflixai.backend.service.OmdbApiException;
import com.netflixai.backend.service.OmdbMovieService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class OmdbMovieServiceTests {

    private static final String BASE_URL = "https://www.omdbapi.com/";

    private MockRestServiceServer server;
    private OmdbMovieService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new OmdbMovieService(builder, "test-key", BASE_URL);
    }

    @Test
    void searchesOmdbAndMapsTitleYearAndAvailablePoster() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(queryParam("s", "The%20Matrix"))
                .andExpect(queryParam("apikey", "test-key"))
                .andExpect(queryParam("type", "movie"))
                .andRespond(withSuccess("""
                        {"Response":"True","totalResults":"1","Search":[
                          {"Title":"The Matrix","Year":"1999","imdbID":"tt0133093","Poster":"https://img.test/matrix.jpg"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        var response = service.search("The Matrix", 1);

        assertEquals(1, response.totalResults());
        assertEquals("tt0133093", response.results().getFirst().imdbId());
        assertEquals(1999, response.results().getFirst().releaseYear());
        assertEquals("https://img.test/matrix.jpg", response.results().getFirst().posterUrl());
        assertNull(response.results().getFirst().plot());
    }

    @Test
    void retrievesDetailsAndTreatsUnavailablePosterAndRatingAsMissing() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(queryParam("i", "tt0133093"))
                .andExpect(queryParam("plot", "full"))
                .andRespond(withSuccess("""
                        {"Response":"True","Title":"The Matrix","Year":"1999","imdbID":"tt0133093",
                         "Plot":"A computer hacker learns the truth.","imdbRating":"8.7",
                         "Poster":"N/A","Runtime":"136 min"}
                        """, MediaType.APPLICATION_JSON));

        OmdbMovieResponse movie = service.getByImdbId("tt0133093");

        assertEquals("The Matrix", movie.title());
        assertEquals(1999, movie.releaseYear());
        assertEquals("A computer hacker learns the truth.", movie.plot());
        assertEquals(new BigDecimal("8.7"), movie.rating());
        assertEquals(136, movie.runtime());
        assertNull(movie.posterUrl());
    }

    @Test
    void titleAndYearLookupUsesBothFieldsAndRejectsInvalidPosterUrls() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(queryParam("t", "The%20Matrix"))
                .andExpect(queryParam("y", "1999"))
                .andRespond(withSuccess("""
                        {"Response":"True","Title":"The Matrix","Year":"1999","imdbID":"tt0133093",
                         "Plot":"A computer hacker learns the truth.","Genre":"Action, Sci-Fi",
                         "imdbRating":"8.7","Poster":"javascript:alert(1)","Runtime":"136 min"}
                        """, MediaType.APPLICATION_JSON));

        OmdbMovieResponse movie = service.findByTitleAndYear("The Matrix", 1999);

        assertEquals("The Matrix", movie.title());
        assertEquals(1999, movie.releaseYear());
        assertEquals(List.of("Action", "Sci-Fi"), movie.genres());
        assertNull(movie.posterUrl());
    }

    @Test
    void returnsAnEmptySearchForOmdbNoMatches() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(queryParam("s", "No%20such%20film"))
                .andRespond(withSuccess("""
                        {"Response":"False","Error":"Movie not found!"}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(List.of(), service.search("No such film", 1).results());
    }

    @Test
    void reportsMissingApiKeyAndUpstreamFailuresWithoutLeakingDetails() {
        OmdbMovieService unconfigured = new OmdbMovieService(RestClient.builder(), "", BASE_URL);
        OmdbApiException missingKey = assertThrows(
                OmdbApiException.class, () -> unconfigured.search("Inception", 1));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, missingKey.status());

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(queryParam("s", "Inception"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        OmdbApiException rateLimited = assertThrows(
                OmdbApiException.class, () -> service.search("Inception", 1));
        assertEquals(HttpStatus.BAD_GATEWAY, rateLimited.status());
        assertEquals("OMDb could not complete the movie request. Please try again later.",
                rateLimited.getMessage());
    }

    @Test
    void doesNotRepeatTitleLookupsImmediatelyAfterTheProviderIsRateLimited() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(queryParam("t", "Unavailable%20Film"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(OmdbApiException.class, () -> service.findByTitleAndYear("Unavailable Film", 2020));
        assertNull(service.findByTitleAndYear("Another Film", 2020));
        server.verify();
    }

    @Test
    void validatesQueriesPagesAndImdbIdsBeforeCallingOmdb() {
        assertThrows(IllegalArgumentException.class, () -> service.search(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> service.search("Inception", 0));
        assertThrows(IllegalArgumentException.class, () -> service.getByImdbId("not-an-id"));
    }
}
