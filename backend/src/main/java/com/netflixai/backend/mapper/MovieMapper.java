package com.netflixai.backend.mapper;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

import com.netflixai.backend.dto.MovieResponse;
import com.netflixai.backend.dto.MovieSummaryResponse;
import com.netflixai.backend.entity.Movie;
import com.netflixai.backend.service.TmdbMovieMetadata;

@Component
public class MovieMapper {

    public MovieResponse toResponse(Movie movie) {
        return toResponse(movie, null);
    }

    public MovieResponse toResponse(Movie movie, TmdbMovieMetadata metadata) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getOverview(),
                movie.getReleaseDate(),
                movie.getReleaseYear(),
                metadataValue(movie.getRuntime(), metadata == null ? null : metadata.runtime()),
                metadataValue(movie.getRating(), metadata == null ? null : metadata.rating()),
                movie.getPosterUrl(),
                movie.getBackdropUrl(),
                metadataValue(movie.getTrailerUrl(), metadata == null ? null : metadata.trailerUrl()),
                metadata == null ? null : metadata.youtubeId(),
                metadataValue(movie.getLanguage(), metadata == null ? null : metadata.language()),
                movie.getCountry(),
                movie.getFeatured(),
                movie.getDemoContent(),
                metadataList(movie.getGenres().stream().map(genre -> genre.getName()).sorted().toList(),
                        metadata == null ? null : metadata.genres()),
                metadataCast(movie, metadata),
                metadataList(movie.getDirectors().stream().map(director -> director.getName()).sorted().toList(),
                        metadata == null ? null : metadata.directors())
        );
    }

    public MovieSummaryResponse toSummary(Movie movie) {
        return new MovieSummaryResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getOverview(),
                movie.getReleaseDate(),
                movie.getReleaseYear(),
                movie.getRuntime(),
                movie.getRating(),
                movie.getPosterUrl(),
                movie.getBackdropUrl(),
                movie.getLanguage(),
                movie.getCountry(),
                movie.getFeatured(),
                movie.getGenres().stream().map(genre -> genre.getName()).sorted().toList()
        );
    }

    private <T> T metadataValue(T storedValue, T metadataValue) {
        if (storedValue != null && (!(storedValue instanceof String text) || !text.isBlank())) {
            return storedValue;
        }
        return metadataValue;
    }

    private List<String> metadataList(List<String> storedValues, List<String> metadataValues) {
        return metadataValues == null || metadataValues.isEmpty() ? storedValues : metadataValues;
    }

    private List<MovieResponse.CastMemberResponse> metadataCast(Movie movie, TmdbMovieMetadata metadata) {
        List<MovieResponse.CastMemberResponse> storedCast = movie.getCastMembers().stream()
                .sorted(Comparator.comparing(member -> member.getName()))
                .map(member -> new MovieResponse.CastMemberResponse(member.getName(), member.getCharacterName()))
                .toList();
        return metadata == null || metadata.cast().isEmpty() ? storedCast : metadata.cast();
    }
}
