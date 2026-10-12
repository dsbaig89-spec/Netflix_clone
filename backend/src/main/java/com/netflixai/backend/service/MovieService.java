package com.netflixai.backend.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.netflixai.backend.dto.MovieCreateRequest;
import com.netflixai.backend.dto.MovieUpdateRequest;
import com.netflixai.backend.entity.CastMember;
import com.netflixai.backend.entity.Director;
import com.netflixai.backend.entity.Genre;
import com.netflixai.backend.entity.Movie;
import com.netflixai.backend.exception.MovieNotFoundException;
import com.netflixai.backend.repository.CastMemberRepository;
import com.netflixai.backend.repository.DirectorRepository;
import com.netflixai.backend.repository.GenreRepository;
import com.netflixai.backend.repository.MovieRepository;

@Service
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final CastMemberRepository castMemberRepository;
    private final DirectorRepository directorRepository;

    public MovieService(MovieRepository movieRepository, GenreRepository genreRepository,
                        CastMemberRepository castMemberRepository, DirectorRepository directorRepository) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.castMemberRepository = castMemberRepository;
        this.directorRepository = directorRepository;
    }

    public Page<Movie> getAll(Pageable pageable) {
        return movieRepository.findAll(pageable);
    }

    public Movie getById(Long id) {
        return movieRepository.findById(id).orElseThrow(() -> new MovieNotFoundException(id));
    }

    public Page<Movie> search(String query, Pageable pageable) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }
        return movieRepository.findDistinctByTitleContainingIgnoreCaseOrOverviewContainingIgnoreCase(
                normalizedQuery, normalizedQuery, pageable);
    }

    public Page<Movie> getByGenre(String genre, Pageable pageable) {
        String normalizedGenre = genre == null ? "" : genre.trim();
        if (normalizedGenre.isEmpty()) {
            throw new IllegalArgumentException("Genre must not be empty");
        }
        return movieRepository.findDistinctByGenresNameIgnoreCase(normalizedGenre, pageable);
    }

    public Page<Movie> getTrending(Pageable pageable) {
        return movieRepository.findTrending(pageable);
    }

    public Page<Movie> getPopular(Pageable pageable) {
        return movieRepository.findPopular(pageable);
    }

    public Page<Movie> getTopRated(Pageable pageable) {
        return movieRepository.findTopRated(pageable);
    }

    @Transactional
    public Movie create(MovieCreateRequest request) {
        Movie movie = new Movie(request.title(), request.overview(), request.releaseDate(), request.releaseYear(),
                request.runtime(), request.rating(), request.posterUrl(), request.backdropUrl(), request.trailerUrl(),
                request.language(), request.country(), request.featured());
        replaceRelationships(movie, request.genres(), request.cast(), request.directors());
        return movieRepository.save(movie);
    }

    @Transactional
    public Movie update(Long id, MovieUpdateRequest request) {
        Movie movie = getById(id);
        movie.updateDetails(request.title(), request.overview(), request.releaseDate(), request.releaseYear(),
                request.runtime(), request.rating(), request.posterUrl(), request.backdropUrl(), request.trailerUrl(),
                request.language(), request.country(), request.featured());
        replaceRelationships(movie, request.genres(), request.cast(), request.directors());
        return movieRepository.save(movie);
    }

    private void replaceRelationships(Movie movie, Set<String> genreNames, Set<String> castNames, Set<String> directorNames) {
        movie.getGenres().clear();
        movie.getCastMembers().clear();
        movie.getDirectors().clear();
        movie.getGenres().addAll(resolveGenres(genreNames));
        movie.getCastMembers().addAll(resolveCast(castNames));
        movie.getDirectors().addAll(resolveDirectors(directorNames));
    }

    private Set<Genre> resolveGenres(Set<String> names) {
        if (names == null) return Collections.emptySet();
        Set<Genre> genres = new LinkedHashSet<>();
        names.stream().map(String::trim).filter(name -> !name.isEmpty()).forEach(name ->
                genres.add(genreRepository.findByNameIgnoreCase(name).orElseGet(() -> genreRepository.save(new Genre(name)))));
        return genres;
    }

    private Set<CastMember> resolveCast(Set<String> names) {
        if (names == null) return Collections.emptySet();
        Set<CastMember> cast = new LinkedHashSet<>();
        names.stream().map(String::trim).filter(name -> !name.isEmpty()).forEach(name ->
                cast.add(castMemberRepository.findByNameIgnoreCase(name).orElseGet(() -> castMemberRepository.save(new CastMember(name, null)))));
        return cast;
    }

    private Set<Director> resolveDirectors(Set<String> names) {
        if (names == null) return Collections.emptySet();
        Set<Director> directors = new LinkedHashSet<>();
        names.stream().map(String::trim).filter(name -> !name.isEmpty()).forEach(name ->
                directors.add(directorRepository.findByNameIgnoreCase(name).orElseGet(() -> directorRepository.save(new Director(name)))));
        return directors;
    }
}
