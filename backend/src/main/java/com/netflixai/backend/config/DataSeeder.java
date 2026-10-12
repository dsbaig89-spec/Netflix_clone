package com.netflixai.backend.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.netflixai.backend.entity.CastMember;
import com.netflixai.backend.entity.Director;
import com.netflixai.backend.entity.Genre;
import com.netflixai.backend.entity.Movie;
import com.netflixai.backend.repository.CastMemberRepository;
import com.netflixai.backend.repository.DirectorRepository;
import com.netflixai.backend.repository.GenreRepository;
import com.netflixai.backend.repository.MovieRepository;

@Configuration
public class DataSeeder {

    @Bean
        @Order(0)
    CommandLineRunner seedMovies(MovieRepository movieRepository, GenreRepository genreRepository,
                                 CastMemberRepository castMemberRepository, DirectorRepository directorRepository) {
        return args -> {
            movieRepository.markPlaceholderSeedMoviesAsDemo();
            if (movieRepository.count() > 0) {
                return;
            }

            Map<String, Genre> genres = new HashMap<>();
            Map<String, CastMember> cast = new HashMap<>();
            Map<String, Director> directors = new HashMap<>();

            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "The Last Signal", "A radio astronomer receives a message from deep space that may change humanity's future.", "2024-03-15", "7.9", true, "Sci-Fi", "Drama", "Maya Chen", "Elena Park");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Paper Moons", "Two strangers map a forgotten coastline while learning to trust each other.", "2023-09-22", "8.1", false, "Drama", "Romance", "Jon Bell", "Rina Sol");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Neon Orchard", "A street photographer uncovers a citywide art conspiracy after dark.", "2022-11-04", "7.6", true, "Thriller", "Mystery", "Ari Vega", "Theo Moss");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Northbound", "A young cartographer crosses a frozen country to deliver one final letter.", "2021-01-29", "7.4", false, "Adventure", "Drama", "Leah Ortiz", "Samir Cole");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Quiet Frequency", "A sound engineer records a melody that appears in dreams around the world.", "2024-06-07", "8.4", true, "Sci-Fi", "Mystery", "Nora Vale", "Elena Park");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "The Small Hours", "Three friends find unexpected courage during one long night in the city.", "2020-08-14", "7.2", false, "Comedy", "Drama", "Owen Reed", "Jon Bell");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Orbit House", "A family-run observatory studies deep space and becomes a refuge for an engineer hiding a secret.", "2022-02-18", "8.0", true, "Sci-Fi", "Family", "Maya Chen", "Rina Sol");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Second Sunrise", "A chef rebuilds a closed diner and discovers a community waiting for a place to gather.", "2019-05-10", "7.8", false, "Romance", "Drama", "Rina Sol", "Leah Ortiz");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Glass Harbor", "A marine biologist investigates unusual lights beneath a quiet fishing town.", "2023-04-28", "8.2", true, "Mystery", "Adventure", "Theo Moss", "Elena Park");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Kindred Engines", "Two rival mechanics team up to enter a legendary desert rally.", "2021-07-16", "7.5", false, "Action", "Comedy", "Samir Cole", "Ari Vega");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Wildflower Code", "A botanist and a software designer decode a pattern hidden in old seed banks.", "2024-01-12", "8.3", true, "Drama", "Sci-Fi", "Nora Vale", "Maya Chen");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "The Lantern Room", "A night-shift librarian helps visitors solve the mysteries in their own stories.", "2018-10-19", "7.1", false, "Drama", "Family", "Owen Reed", "Jon Bell");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Tide Lines", "A rescue diver returns home when a storm reveals a path through the old sea wall.", "2020-03-06", "7.7", false, "Adventure", "Action", "Leah Ortiz", "Theo Moss");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "After the Rain", "An urban gardener turns an abandoned roof into a living meeting place.", "2022-09-09", "7.3", false, "Romance", "Drama", "Rina Sol", "Samir Cole");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Signal Fires", "A volunteer radio crew keeps a mountain town connected through a winter blackout.", "2021-12-03", "8.5", true, "Thriller", "Drama", "Ari Vega", "Maya Chen");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Bright Matter", "A physics teacher invites students to build a machine that measures wonder.", "2019-02-15", "7.9", false, "Sci-Fi", "Family", "Maya Chen", "Elena Park");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Blue Static", "A touring musician follows an anonymous recording across three small towns.", "2023-12-01", "7.6", false, "Music", "Drama", "Nora Vale", "Rina Sol");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Common Ground", "Neighbors build a community kitchen when their favorite market closes.", "2020-11-20", "7.8", false, "Comedy", "Family", "Owen Reed", "Leah Ortiz");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "The Far Room", "A restoration artist finds a hidden room behind a portrait in an old hotel.", "2017-06-23", "7.0", false, "Mystery", "Thriller", "Theo Moss", "Jon Bell");
            seed(movieRepository, genreRepository, castMemberRepository, directorRepository, genres, cast, directors,
                    "Mosaic Sky", "A pilot and an architect design a floating observatory above a changing coastline.", "2024-08-16", "8.6", true, "Adventure", "Sci-Fi", "Leah Ortiz", "Samir Cole");
        };
    }

    private void seed(MovieRepository movieRepository, GenreRepository genreRepository,
                      CastMemberRepository castMemberRepository, DirectorRepository directorRepository,
                      Map<String, Genre> genres, Map<String, CastMember> cast, Map<String, Director> directors,
                      String title, String overview, String releaseDate, String rating, boolean featured,
                      String genreOne, String genreTwo, String castName, String directorName) {
        Movie movie = new Movie(title, overview, LocalDate.parse(releaseDate), LocalDate.parse(releaseDate).getYear(),
                90 + title.length(), new BigDecimal(rating), posterUrl(title), backdropUrl(title),
                null, "English", "United States", featured);
        movie.setDemoContent(true);
        movie.getGenres().add(genre(genreOne, genreRepository, genres));
        movie.getGenres().add(genre(genreTwo, genreRepository, genres));
        movie.getCastMembers().add(actor(castName, castMemberRepository, cast));
        movie.getDirectors().add(director(directorName, directorRepository, directors));
        movieRepository.save(movie);
    }

    private Genre genre(String name, GenreRepository repository, Map<String, Genre> cache) {
        return cache.computeIfAbsent(name, key -> repository.findByNameIgnoreCase(key).orElseGet(() -> repository.save(new Genre(key))));
    }

    private CastMember actor(String name, CastMemberRepository repository, Map<String, CastMember> cache) {
        return cache.computeIfAbsent(name, key -> repository.findByNameIgnoreCase(key).orElseGet(() -> repository.save(new CastMember(key, "Lead"))));
    }

    private Director director(String name, DirectorRepository repository, Map<String, Director> cache) {
        return cache.computeIfAbsent(name, key -> repository.findByNameIgnoreCase(key).orElseGet(() -> repository.save(new Director(key))));
    }

    private String posterUrl(String title) {
        return "https://placehold.co/600x900/141414/ffffff?text=" + title.replace(' ', '+');
    }

    private String backdropUrl(String title) {
        return "https://placehold.co/1600x900/202b31/ffffff?text=" + title.replace(' ', '+');
    }
}
