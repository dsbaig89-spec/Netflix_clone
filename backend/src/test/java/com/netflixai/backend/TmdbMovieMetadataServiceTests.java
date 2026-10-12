package com.netflixai.backend;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.netflixai.backend.entity.Movie;
import com.netflixai.backend.service.TmdbMovieMetadata;
import com.netflixai.backend.service.TmdbMovieMetadataService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TmdbMovieMetadataServiceTests {

    private static final String BASE_URL = "https://api.themoviedb.org/3";

    private MockRestServiceServer server;
    private TmdbMovieMetadataService metadataService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        metadataService = new TmdbMovieMetadataService(builder, "test-key", "", BASE_URL);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void matchesTheMovieByTitleAndYearAndSelectsAnOfficialYoutubeTrailer() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search/movie")))
                .andRespond(withSuccess("""
                        {"results":[
                          {"id":22,"title":"A Silent Voice: The Movie","release_date":"2016-09-17"},
                          {"id":23,"title":"A Silent Voice","release_date":"2020-09-17"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("/movie/22"),
                        org.hamcrest.Matchers.containsString("append_to_response=credits"),
                        org.hamcrest.Matchers.containsString("api_key=test-key"))))
                .andRespond(withSuccess("""
                        {
                          "runtime":130,
                          "vote_average":8.1,
                          "vote_count":2500,
                          "original_language":"ja",
                          "genres":[{"name":"Animation"},{"name":"Drama"}],
                          "credits":{
                            "crew":[{"job":"Director","name":"Naoko Yamada"}],
                            "cast":[{"name":"Miyu Irino","character":"Shoya Ishida"}]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("/movie/22/videos"),
                        org.hamcrest.Matchers.containsString("api_key=test-key"))))
                .andRespond(withSuccess("""
                        {"results":[
                          {"site":"YouTube","type":"Trailer","official":false,"key":"invalid_key"},
                          {"site":"YouTube","type":"Trailer","official":true,"key":"abcdefghijk"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        TmdbMovieMetadata metadata = metadataService.findMetadata(movie("A Silent Voice", 2016));

        assertNotNull(metadata);
        assertEquals(new BigDecimal("8.1"), metadata.rating());
        assertEquals(130, metadata.runtime());
        assertEquals("Japanese", metadata.language());
        assertEquals(List.of("Animation", "Drama"), metadata.genres());
        assertEquals(List.of("Naoko Yamada"), metadata.directors());
        assertEquals("Miyu Irino", metadata.cast().getFirst().name());
        assertEquals("abcdefghijk", metadata.youtubeId());
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", metadata.trailerUrl());
        assertFalse(metadata.youtubeId().equals("invalid_key"));
    }

    @Test
    void authenticatesWithTmdbReadAccessTokenWithoutPuttingItInTheUrl() {
        RestClient.Builder tokenBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(tokenBuilder).build();
        metadataService = new TmdbMovieMetadataService(tokenBuilder, "", "test-access-token", BASE_URL);
        server.expect(requestTo(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("/search/movie"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("api_key")))))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-access-token"))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        assertNull(metadataService.findMetadata(movie("Interstellar", 2014)));
    }

    @Test
    void returnsNoMetadataWhenTitleAndYearDoNotMatch() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search/movie")))
                .andRespond(withSuccess("""
                        {"results":[{"id":27205,"title":"Inception","release_date":"2011-07-15"}]}
                        """, MediaType.APPLICATION_JSON));

        assertNull(metadataService.findMetadata(movie("Inception", 2010)));
    }

    @Test
    void handlesTmdbRateLimitsWithoutFailingTheMovieRequest() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search/movie")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertNull(metadataService.findMetadata(movie("Interstellar", 2014)));
        assertNull(metadataService.findMetadata(movie("Interstellar", 2014)));
    }

    private Movie movie(String title, int year) {
        return new Movie(title, null, null, year, null, null, null, null, null, null, null, false);
    }
}
