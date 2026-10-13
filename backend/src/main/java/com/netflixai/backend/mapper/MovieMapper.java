package com.netflixai.backend.mapper;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

import com.netflixai.backend.dto.MovieResponse;
import com.netflixai.backend.dto.MovieSummaryResponse;
import com.netflixai.backend.entity.Movie;
import com.netflixai.backend.service.MovieMetadataResolver;
import com.netflixai.backend.service.TrailerResolutionService;

@Component
public class MovieMapper {

    private final MovieMetadataResolver metadataResolver;
    private final TrailerResolutionService trailerResolutionService;

    public MovieMapper(MovieMetadataResolver metadataResolver, TrailerResolutionService trailerResolutionService) {
        this.metadataResolver = metadataResolver;
        this.trailerResolutionService = trailerResolutionService;
    }

    public MovieResponse toResponse(Movie movie) {
        MovieMetadataResolver.ResolvedMetadata metadata = metadataResolver.resolve(movie);
        TrailerResolutionService.Trailer trailer = trailerResolutionService.resolve(movie);
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                metadata.overview(),
                movie.getReleaseDate(),
                metadata.releaseYear(),
                metadata.runtime(),
                metadata.rating(),
                metadata.posterUrl(),
                movie.getBackdropUrl(),
                trailer == null ? null : trailer.trailerUrl(),
                trailer == null ? null : trailer.youtubeId(),
                movie.getLanguage(),
                movie.getCountry(),
                movie.getFeatured(),
                movie.getDemoContent(),
                metadata.genres(),
                movie.getCastMembers().stream()
                        .sorted(Comparator.comparing(member -> member.getName()))
                        .map(member -> new MovieResponse.CastMemberResponse(
                                member.getName(), member.getCharacterName()))
                        .toList(),
                movie.getDirectors().stream().map(director -> director.getName()).sorted().toList()
        );
    }

    public MovieSummaryResponse toSummary(Movie movie) {
        MovieMetadataResolver.ResolvedMetadata metadata = metadataResolver.resolve(movie);
        TrailerResolutionService.Trailer trailer = trailerResolutionService.resolve(movie);
        return new MovieSummaryResponse(
                movie.getId(),
                movie.getTitle(),
                metadata.overview(),
                movie.getReleaseDate(),
                metadata.releaseYear(),
                metadata.runtime(),
                metadata.rating(),
                metadata.posterUrl(),
                movie.getBackdropUrl(),
                movie.getLanguage(),
                movie.getCountry(),
                movie.getFeatured(),
                metadata.genres(),
                trailer == null ? null : trailer.trailerUrl(),
                trailer == null ? null : trailer.youtubeId()
        );
    }

}
