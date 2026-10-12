package com.netflixai.backend.controller;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netflixai.backend.dto.MovieResponse;
import com.netflixai.backend.dto.MovieSummaryResponse;
import com.netflixai.backend.dto.PageResponse;
import com.netflixai.backend.mapper.MovieMapper;
import com.netflixai.backend.service.MovieService;
import com.netflixai.backend.service.TmdbMovieMetadataService;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "rating", "releaseDate", "releaseYear");

    private final MovieService movieService;
    private final MovieMapper movieMapper;
    private final TmdbMovieMetadataService tmdbMovieMetadataService;

    public MovieController(MovieService movieService, MovieMapper movieMapper,
                           TmdbMovieMetadataService tmdbMovieMetadataService) {
        this.movieService = movieService;
        this.movieMapper = movieMapper;
        this.tmdbMovieMetadataService = tmdbMovieMetadataService;
    }

    @GetMapping
    public PageResponse<MovieSummaryResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort) {
        return toPageResponse(movieService.getAll(pageable(page, size, sort)));
    }

    @GetMapping("/{id}")
    public MovieResponse getById(@PathVariable Long id) {
        var movie = movieService.getById(id);
        return movieMapper.toResponse(movie, tmdbMovieMetadataService.findMetadata(movie));
    }

    @GetMapping("/search")
    public PageResponse<MovieSummaryResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort) {
        return toPageResponse(movieService.search(query, pageable(page, size, sort)));
    }

    @GetMapping("/genre/{genre}")
    public PageResponse<MovieSummaryResponse> getByGenre(
            @PathVariable String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort) {
        return toPageResponse(movieService.getByGenre(genre, pageable(page, size, sort)));
    }

    @GetMapping("/trending")
    public PageResponse<MovieSummaryResponse> getTrending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return toPageResponse(movieService.getTrending(PageRequest.of(page, validatedSize(size))));
    }

    @GetMapping("/popular")
    public PageResponse<MovieSummaryResponse> getPopular(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return toPageResponse(movieService.getPopular(PageRequest.of(page, validatedSize(size))));
    }

    @GetMapping("/top-rated")
    public PageResponse<MovieSummaryResponse> getTopRated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return toPageResponse(movieService.getTopRated(PageRequest.of(page, validatedSize(size))));
    }

    private Pageable pageable(int page, int size, String sort) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        String[] sortParts = sort.split(",", -1);
        if (sortParts.length != 2 || !ALLOWED_SORT_FIELDS.contains(sortParts[0])) {
            throw new IllegalArgumentException("Sort must use an allowed field and direction");
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortParts[1]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Sort direction must be asc or desc");
        }
        return PageRequest.of(page, validatedSize(size), Sort.by(direction, sortParts[0]));
    }

    private int validatedSize(int size) {
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        return size;
    }

    private PageResponse<MovieSummaryResponse> toPageResponse(Page<?> page) {
        @SuppressWarnings("unchecked")
        Page<com.netflixai.backend.entity.Movie> moviePage = (Page<com.netflixai.backend.entity.Movie>) page;
        return PageResponse.from(moviePage.map(movieMapper::toSummary));
    }
}
