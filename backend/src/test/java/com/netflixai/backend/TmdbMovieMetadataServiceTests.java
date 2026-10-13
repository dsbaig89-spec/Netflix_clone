package com.netflixai.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatus;

import com.netflixai.backend.entity.Movie;
import com.netflixai.backend.service.TmdbMovieMetadataService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TmdbMovieMetadataServiceTests {

    private static final String BASE_URL = "https://tmdb.test/3";

    private MockRestServiceServer server;
    private TmdbMovieMetadataService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new TmdbMovieMetadataService(builder, "test-key", "", BASE_URL);
    }

    @Test
    void resolvesMetadataPosterAndOfficialTrailerWithMatchingTitleAndYearAndCachesResult() {
        expectMetadataRequests();

        Movie movie = movie("Exact Title", 2020);
        var first = service.findMetadata(movie);
        var second = service.findMetadata(movie);

        assertNotNull(first);
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", first.posterUrl());
        assertEquals("Verified overview", first.overview());
        assertEquals("abcdefghijk", first.youtubeId());
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", first.trailerUrl());
        assertEquals(first, second);
        server.verify();
    }

    @Test
    void rejectsSimilarMovieTitleAndDoesNotConstructUrlForNullPosterPath() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search/movie")))
                .andRespond(withSuccess("""
                        {"results":[{"id":45,"title":"Exact Title: The Sequel","release_date":"2020-01-02"}]}
                        """, MediaType.APPLICATION_JSON));

        assertNull(service.findMetadata(movie("Exact Title", 2020)));
        server.verify();
    }

    @Test
    void matchesSameTitleByReleaseYearAndLeavesMissingPosterAndTrailerNull() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search/movie")))
                .andRespond(withSuccess("""
                        {"results":[
                          {"id":40,"title":"Exact Title","release_date":"2019-01-02"},
                          {"id":41,"title":"Exact Title","release_date":"2020-01-02"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/movie/41")))
                .andRespond(withSuccess("""
                        {"vote_count":0,"runtime":null,"original_language":"en","genres":[],
                         "poster_path":null,"overview":"A verified overview.",
                         "credits":{"crew":[],"cast":[]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/movie/41/videos")))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        var result = service.findMetadata(movie("Exact Title", 2020));

        assertNotNull(result);
        assertNull(result.posterUrl());
        assertNull(result.youtubeId());
        assertNull(result.trailerUrl());
        server.verify();
    }

    @Test
    void doesNotCallTmdbWithoutCredentials() {
        TmdbMovieMetadataService unconfigured = new TmdbMovieMetadataService(
                RestClient.builder(), "", "", BASE_URL);

        assertNull(unconfigured.findMetadata(movie("Exact Title", 2020)));
        server.verify();
    }

    @Test
    void cachesProviderRateLimitFailureAndReturnsNoUnverifiedMetadata() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search/movie")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        Movie movie = movie("Rate Limited Film", 2020);

        assertNull(service.findMetadata(movie));
        assertNull(service.findMetadata(movie));
        assertNull(service.findMetadata(movie("Different Film", 2019)));
        server.verify();
    }

    private void expectMetadataRequests() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search/movie")))
                .andRespond(withSuccess("""
                        {"results":[
                          {"id":40,"title":"Exact Title","release_date":"2019-01-02"},
                          {"id":41,"title":"Exact Title","release_date":"2020-01-02"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/movie/41")))
                .andRespond(withSuccess("""
                        {"vote_count":25,"vote_average":8.1,"runtime":120,"original_language":"en",
                         "genres":[{"name":"Drama"}],"poster_path":"/poster.jpg",
                         "overview":"Verified overview",
                         "credits":{"crew":[{"job":"Director","name":"Verified Director"}],
                                    "cast":[{"name":"Verified Actor","character":"Lead"}]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/movie/41/videos")))
                .andRespond(withSuccess("""
                        {"results":[
                          {"site":"YouTube","type":"Trailer","key":"zyxwvutsrqp","official":false},
                          {"site":"YouTube","type":"Trailer","key":"abcdefghijk","official":true},
                          {"site":"Vimeo","type":"Trailer","key":"mnopqrstuvw","official":true}
                        ]}
                        """, MediaType.APPLICATION_JSON));
    }

    private Movie movie(String title, int year) {
        return new Movie(title, null, null, year, null, null, null, null, null, null, null, false);
    }
}
