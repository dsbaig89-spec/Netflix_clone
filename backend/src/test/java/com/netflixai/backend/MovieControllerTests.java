package com.netflixai.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MovieControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMoviesSupportsPaginationAndSorting() throws Exception {
        mockMvc.perform(get("/api/movies?page=0&size=5&sort=rating,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(95))
                .andExpect(jsonPath("$.totalPages").value(19))
                .andExpect(jsonPath("$.content[0].title").value("Mosaic Sky"));
    }

        @Test
        void getMoviesReturnsTheCompleteCatalogueWhenPageSizeCoversIt() throws Exception {
                mockMvc.perform(get("/api/movies?page=0&size=100&sort=title,asc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(95))
                                .andExpect(jsonPath("$.totalElements").value(95))
                                .andExpect(jsonPath("$.totalPages").value(1));
        }

    @Test
    void getMovieByIdReturnsDetailedMovie() throws Exception {
        mockMvc.perform(get("/api/movies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.genres").isArray())
                .andExpect(jsonPath("$.cast").isArray())
                .andExpect(jsonPath("$.directors").isArray())
                .andExpect(jsonPath("$.demoContent").value(true))
                .andExpect(jsonPath("$.trailerUrl").doesNotExist());
    }

    @Test
    void trailerEndpointReturnsAResolutionStateForTheRequestedMovie() throws Exception {
        mockMvc.perform(get("/api/movies/1/trailer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value(1))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.available").exists());
    }

    @Test
    void curatedCatalogueContainsRequestedFilmsWithoutRatingOrTrailerClaims() throws Exception {
        mockMvc.perform(get("/api/movies/search?query=Parasite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Parasite"))
                .andExpect(jsonPath("$.content[0].releaseYear").value(2019))
                .andExpect(jsonPath("$.content[0].language").value("Korean"));

        mockMvc.perform(get("/api/movies/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Inception"))
                .andExpect(jsonPath("$.rating").doesNotExist())
                .andExpect(jsonPath("$.runtime").doesNotExist())
                .andExpect(jsonPath("$.trailerUrl").doesNotExist())
                .andExpect(jsonPath("$.youtubeId").doesNotExist())
                .andExpect(jsonPath("$.genres.length()").value(3))
                .andExpect(jsonPath("$.directors.length()").value(0))
                .andExpect(jsonPath("$.cast.length()").value(0))
                .andExpect(jsonPath("$.language").value("English"));
    }

    @Test
    void requestedMoviesReturnTheirOwnExistingPosterUrls() throws Exception {
        Map<String, String> expectedPosters = Map.of(
                "3 Idiots", "https://image.tmdb.org/t/p/w500/66A9MqXOyVFCssoloscw79z8Tew.jpg",
                "777 Charlie", "https://image.tmdb.org/t/p/w500/qArPmXH0aWsT3SEtYl8XrU2Oz48.jpg",
                "96", "https://image.tmdb.org/t/p/w500/nrVloCa2hCFOztRF1DZU2jnWIiQ.jpg",
                "A Silent Voice", "https://image.tmdb.org/t/p/w500/tuFaWiqX0TXoWu7DGNcmX3UW7sT.jpg",
                "Inception", "https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
                "Interstellar", "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
                "The Dark Knight", "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg"
        );
        Set<String> actualPosters = new HashSet<>();

        for (Map.Entry<String, String> expected : expectedPosters.entrySet()) {
            mockMvc.perform(get("/api/movies/search").param("query", expected.getKey()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value(expected.getKey()))
                    .andExpect(jsonPath("$.content[0].posterUrl").value(expected.getValue()));
            actualPosters.add(expected.getValue());
        }

        org.junit.jupiter.api.Assertions.assertEquals(expectedPosters.size(), actualPosters.size());
    }

    @Test
    void searchIsCaseInsensitiveAndUsesMovieMetadata() throws Exception {
        mockMvc.perform(get("/api/movies/search?query=SPACE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void genreFilterIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/movies/genre/sCi-Fi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @Test
    void rankingEndpointsReturnMovies() throws Exception {
        mockMvc.perform(get("/api/movies/top-rated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Mosaic Sky"));
        mockMvc.perform(get("/api/movies/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mockMvc.perform(get("/api/movies/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Mosaic Sky"));
    }

    @Test
    void blankSearchReturnsBadRequest() throws Exception {
                mockMvc.perform(get("/api/movies/search").param("query", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void missingMovieReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/movies/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MOVIE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Movie not found with id: 999999"));
    }

    @Test
    void invalidPageSizeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/movies?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void omdbEndpointsReportMissingServerConfigurationSafely() throws Exception {
        mockMvc.perform(get("/api/movies/omdb/search").param("query", "Inception"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("OMDB_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.message").value("OMDb search is not configured on the server"));

        mockMvc.perform(get("/api/movies/omdb/not-an-imdb-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }
}
