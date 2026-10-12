package com.netflixai.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.netflixai.backend.entity.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    @Modifying
    @Transactional
    @Query("update Movie movie set movie.demoContent = true, movie.trailerUrl = null where movie.posterUrl like 'https://placehold.co/600x900/%'")
    int markPlaceholderSeedMoviesAsDemo();

    @Override
    @EntityGraph(attributePaths = {"genres", "castMembers", "directors"})
    Page<Movie> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"genres", "castMembers", "directors"})
    java.util.Optional<Movie> findById(Long id);

    java.util.Optional<Movie> findFirstByTitleIgnoreCase(String title);

    @EntityGraph(attributePaths = {"genres", "castMembers", "directors"})
    Page<Movie> findDistinctByTitleContainingIgnoreCaseOrOverviewContainingIgnoreCase(
            String title, String overview, Pageable pageable);

    @EntityGraph(attributePaths = {"genres", "castMembers", "directors"})
    Page<Movie> findDistinctByGenresNameIgnoreCase(String genre, Pageable pageable);

    @EntityGraph(attributePaths = {"genres", "castMembers", "directors"})
    @Query("select m from Movie m order by m.rating desc, m.releaseDate desc")
    Page<Movie> findTopRated(Pageable pageable);

    @EntityGraph(attributePaths = {"genres", "castMembers", "directors"})
    @Query("select m from Movie m order by m.rating desc, m.releaseDate desc")
    Page<Movie> findPopular(Pageable pageable);

    @EntityGraph(attributePaths = {"genres", "castMembers", "directors"})
    @Query("select m from Movie m order by m.featured desc, m.rating desc, m.releaseDate desc")
    Page<Movie> findTrending(Pageable pageable);

    @Query("select count(m) from Movie m where m.title = :title")
    long countByExactTitle(@Param("title") String title);
}
