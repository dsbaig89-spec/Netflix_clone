package com.netflixai.backend.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.netflixai.backend.entity.Genre;
import com.netflixai.backend.entity.Movie;
import com.netflixai.backend.repository.GenreRepository;
import com.netflixai.backend.repository.MovieRepository;

@Configuration
public class CuratedMovieCatalogSeeder {

    private static final List<CatalogMovie> CATALOG = List.of(
            movie("Inception", 2010, "English", "A thief who steals secrets through shared dreaming is tasked with planting an idea instead.", "Action", "Science Fiction", "Thriller"),
            movie("Interstellar", 2014, "English", "A former pilot joins a mission beyond Earth to search for a future for humanity.", "Adventure", "Drama", "Science Fiction"),
            movie("The Dark Knight", 2008, "English", "Batman faces the Joker, whose campaign of chaos pushes Gotham toward its limits.", "Action", "Crime", "Drama"),
            movie("The Dark Knight Rises", 2012, "English", "Batman returns to Gotham when the masked Bane threatens the city.", "Action", "Drama", "Thriller"),
            movie("Avengers: Endgame", 2019, "English", "The surviving Avengers attempt to undo the devastation caused by Thanos.", "Action", "Adventure", "Science Fiction"),
            movie("Avengers: Infinity War", 2018, "English", "The Avengers and their allies confront Thanos as he seeks the Infinity Stones.", "Action", "Adventure", "Science Fiction"),
            movie("Jurassic Park", 1993, "English", "Visitors tour a wildlife park where cloned dinosaurs escape their enclosures.", "Adventure", "Science Fiction", "Thriller"),
            movie("Titanic", 1997, "English", "A romance unfolds aboard the RMS Titanic during its ill-fated maiden voyage.", "Drama", "Romance"),
            movie("The Notebook", 2004, "English", "A man recounts the enduring romance between two young people from different backgrounds.", "Drama", "Romance"),
            movie("The Conjuring", 2013, "English", "Paranormal investigators help a family troubled by a presence in their farmhouse.", "Horror", "Mystery", "Thriller"),
            movie("Avatar", 2009, "English", "A former Marine becomes involved in a conflict on the moon Pandora.", "Action", "Adventure", "Fantasy", "Science Fiction"),
            movie("The Matrix", 1999, "English", "A computer programmer discovers that his world is an elaborate simulation.", "Action", "Science Fiction"),
            movie("The Shawshank Redemption", 1994, "English", "Two prisoners form a lasting friendship while serving sentences at Shawshank prison.", "Drama"),
            movie("The Lord of the Rings: The Fellowship of the Ring", 2001, "English", "A hobbit begins a perilous journey to destroy a powerful ring.", "Adventure", "Drama", "Fantasy"),
            movie("Top Gun: Maverick", 2022, "English", "Maverick trains a group of elite pilots for a dangerous specialized mission.", "Action", "Drama"),
            movie("The Martian", 2015, "English", "An astronaut stranded on Mars works to survive while a rescue is planned.", "Adventure", "Drama", "Science Fiction"),
            movie("Dune", 2021, "English", "Paul Atreides travels with his family to the desert planet Arrakis.", "Adventure", "Drama", "Science Fiction"),
            movie("Dune: Part Two", 2024, "English", "Paul Atreides joins Chani and the Fremen while pursuing a path of revenge.", "Action", "Adventure", "Drama", "Science Fiction"),
            movie("Spider-Man: No Way Home", 2021, "English", "Peter Parker seeks help after his identity as Spider-Man is publicly revealed.", "Action", "Adventure", "Fantasy", "Science Fiction"),
            movie("Inside Out 2", 2024, "English", "Riley's emotions encounter new arrivals as she enters her teenage years.", "Animation", "Adventure", "Comedy", "Family"),

            movie("3 Idiots", 2009, "Hindi", "Three engineering students challenge expectations while navigating college and friendship.", "Comedy", "Drama"),
            movie("Dangal", 2016, "Hindi", "A former wrestler trains his daughters to compete in the sport.", "Drama", "Family", "Sport"),
            movie("Zindagi Na Milegi Dobara", 2011, "Hindi", "Three friends take a road trip through Spain before one of them gets married.", "Adventure", "Comedy", "Drama"),
            movie("Chennai Express", 2013, "Hindi", "A man traveling to immerse his grandfather's ashes becomes entangled in a family conflict.", "Action", "Comedy", "Romance"),
            movie("Lagaan", 2001, "Hindi", "Villagers challenge British officers to a cricket match to contest an oppressive tax.", "Drama", "Musical", "Sport"),
            movie("Taare Zameen Par", 2007, "Hindi", "An art teacher helps a young student who struggles with school and dyslexia.", "Drama", "Family"),
            movie("Drishyam 2", 2022, "Hindi", "A family faces renewed scrutiny years after a crime they concealed.", "Crime", "Drama", "Thriller"),
            movie("Andhadhun", 2018, "Hindi", "A piano player becomes entangled in a murder case after witnessing a crime.", "Comedy", "Crime", "Thriller"),
            movie("Queen", 2013, "Hindi", "A young woman goes on her planned honeymoon alone after her wedding is called off.", "Comedy", "Drama"),
            movie("Bajrangi Bhaijaan", 2015, "Hindi", "A man helps a lost Pakistani girl reunite with her family across the border.", "Adventure", "Comedy", "Drama"),

            movie("K.G.F: Chapter 1", 2018, "Kannada", "Rocky rises through the criminal underworld of the Kolar Gold Fields.", "Action", "Crime", "Drama"),
            movie("K.G.F: Chapter 2", 2022, "Kannada", "Rocky consolidates his power in the Kolar Gold Fields while facing new enemies.", "Action", "Crime", "Drama"),
            movie("Kantara", 2022, "Kannada", "A conflict over forest land brings local traditions and authority into collision.", "Action", "Drama", "Thriller"),
            movie("777 Charlie", 2022, "Kannada", "A solitary factory worker's life changes after he encounters a dog named Charlie.", "Adventure", "Comedy", "Drama"),
            movie("Vikrant Rona", 2022, "Kannada", "A police officer investigates mysterious events in a remote village.", "Action", "Fantasy", "Thriller"),
            movie("Ugramm", 2014, "Kannada", "A man returns to a violent region and becomes involved in its local conflicts.", "Action", "Crime", "Drama"),
            movie("Lucia", 2013, "Kannada", "A theatre usher's life blurs with the dreams and identity of another man.", "Drama", "Mystery", "Science Fiction"),
            movie("Dia", 2020, "Kannada", "A young woman's life is changed by an unexpected relationship and loss.", "Drama", "Romance"),
            movie("Sapta Sagaradaache Ello - Side A", 2023, "Kannada", "A man's love story is tested by circumstance and a life-changing decision.", "Drama", "Romance"),
            movie("Sapta Sagaradaache Ello - Side B", 2023, "Kannada", "Years later, the consequences of a past decision reshape two lives.", "Drama", "Romance"),

            movie("RRR", 2022, "Telugu", "Two men form a powerful friendship amid resistance to British colonial rule.", "Action", "Drama"),
            movie("Baahubali: The Beginning", 2015, "Telugu", "A young man discovers his connection to the kingdom of Mahishmati.", "Action", "Adventure", "Fantasy"),
            movie("Baahubali 2: The Conclusion", 2017, "Telugu", "The story reveals the events behind the fall of Amarendra Baahubali.", "Action", "Adventure", "Fantasy"),
            movie("Pushpa: The Rise", 2021, "Telugu", "A laborer rises through the smuggling trade in red sandalwood.", "Action", "Crime", "Drama"),
            movie("Pushpa 2: The Rule", 2024, "Telugu", "Pushpa faces rivals and authorities as his influence in the smuggling world grows.", "Action", "Crime", "Drama"),
            movie("Eega", 2012, "Telugu", "A murdered man is reincarnated as a fly and seeks to protect the woman he loves.", "Action", "Fantasy", "Romance"),
            movie("Magadheera", 2009, "Telugu", "A modern-day man discovers a connection to a warrior from a past life.", "Action", "Drama", "Fantasy"),
            movie("Hi Nanna", 2023, "Telugu", "A single father and his daughter meet a woman who changes their lives.", "Drama", "Family", "Romance"),
            movie("Sita Ramam", 2022, "Telugu", "A soldier receives letters from a woman and searches for her after their correspondence ends.", "Drama", "Romance"),
            movie("Arjun Reddy", 2017, "Telugu", "A surgeon's life unravels after he loses the woman he loves.", "Drama", "Romance"),

            movie("Vikram", 2022, "Tamil", "A black-ops team investigates a series of murders tied to a drug syndicate.", "Action", "Crime", "Thriller"),
            movie("Jailer", 2023, "Tamil", "A retired jailer takes action after a criminal network threatens his family.", "Action", "Comedy", "Crime"),
            movie("Kaithi", 2019, "Tamil", "An ex-convict helps police transport seized drugs while trying to see his daughter.", "Action", "Crime", "Thriller"),
            movie("Ponniyin Selvan: Part I", 2022, "Tamil", "Political intrigue unfolds in the Chola kingdom as a messenger carries urgent news.", "Action", "Adventure", "Drama"),
            movie("Ponniyin Selvan: Part II", 2023, "Tamil", "The struggle for power in the Chola kingdom continues.", "Action", "Adventure", "Drama"),
            movie("96", 2018, "Tamil", "Former classmates reunite and revisit memories of their school years.", "Drama", "Romance"),
            movie("Soorarai Pottru", 2020, "Tamil", "An aspiring entrepreneur works to make air travel accessible to ordinary people.", "Drama"),
            movie("Maharaja", 2024, "Tamil", "A barber reports a theft and pursues answers about the missing object.", "Action", "Crime", "Drama"),

            movie("Manjummel Boys", 2024, "Malayalam", "A group of friends from Manjummel travel to Kodaikanal, where one falls into a cave.", "Adventure", "Drama", "Thriller"),
            movie("Premam", 2015, "Malayalam", "A young man's romantic life unfolds across three stages of his life.", "Comedy", "Drama", "Romance"),
            movie("Drishyam", 2013, "Malayalam", "A family man uses his resourcefulness to protect his family after a crime.", "Crime", "Drama", "Thriller"),
            movie("Bangalore Days", 2014, "Malayalam", "Three cousins move to Bangalore and navigate love, work, and family.", "Comedy", "Drama", "Romance"),
            movie("2018", 2023, "Malayalam", "Residents of Kerala respond to the devastating floods of 2018.", "Drama", "Thriller"),
            movie("Aavesham", 2024, "Malayalam", "Three college students seek help from a local gangster after being bullied.", "Action", "Comedy"),

            movie("Parasite", 2019, "Korean", "A poor family gradually enters the household and working lives of a wealthy family.", "Comedy", "Drama", "Thriller"),
            movie("Train to Busan", 2016, "Korean", "Passengers struggle to survive a zombie outbreak aboard a train to Busan.", "Action", "Horror", "Thriller"),
            movie("Oldboy", 2003, "Korean", "A man seeks answers and revenge after being imprisoned for years without explanation.", "Action", "Mystery", "Thriller"),
            movie("Extreme Job", 2019, "Korean", "A narcotics team runs a fried-chicken restaurant while surveilling a criminal group.", "Action", "Comedy", "Crime"),
            movie("The Handmaiden", 2016, "Korean", "A handmaiden and a Japanese heiress become involved in a plan built on deception.", "Drama", "Romance", "Thriller"),

            movie("Your Name", 2016, "Japanese", "Two teenagers discover they are mysteriously switching bodies across distance and time.", "Animation", "Drama", "Fantasy", "Romance"),
            movie("Spirited Away", 2001, "Japanese", "A girl enters a spirit world and works to free her parents and return home.", "Animation", "Adventure", "Family", "Fantasy"),
            movie("Suzume", 2022, "Japanese", "A teenage girl travels across Japan to close mysterious doors linked to disasters.", "Animation", "Adventure", "Drama", "Fantasy"),
            movie("A Silent Voice", 2016, "Japanese", "A former bully seeks to make amends with a deaf classmate he once mistreated.", "Animation", "Drama", "Romance"),
            movie("Howl's Moving Castle", 2004, "Japanese", "A young woman transformed by a curse finds refuge in a wizard's moving castle.", "Animation", "Adventure", "Fantasy"),
            movie("Princess Mononoke", 1997, "Japanese", "A young warrior becomes caught in a conflict between a forest and a mining settlement.", "Animation", "Adventure", "Fantasy")
    );

    @Bean
    @Order(1)
    CommandLineRunner seedCuratedMovieCatalog(MovieRepository movieRepository, GenreRepository genreRepository) {
        return args -> {
            Map<String, Genre> genres = new HashMap<>();
            for (CatalogMovie entry : CATALOG) {
                Movie existingMovie = movieRepository.findFirstByTitleIgnoreCase(entry.title()).orElse(null);
                String posterUrl = CuratedMoviePosterCatalog.findPosterUrl(entry.title(), entry.year());
                if (existingMovie != null) {
                    if (posterUrl != null && (existingMovie.getPosterUrl() == null || existingMovie.getPosterUrl().isBlank())) {
                        existingMovie.setPosterUrl(posterUrl);
                        movieRepository.save(existingMovie);
                    }
                    continue;
                }
                Movie movie = new Movie(entry.title(), entry.overview(), null, entry.year(), null, null,
                        posterUrl, null, null, entry.language(), null, false);
                for (String genreName : entry.genres()) {
                    Genre genre = genres.computeIfAbsent(genreName.toLowerCase(), key -> genreRepository
                            .findByNameIgnoreCase(genreName).orElseGet(() -> genreRepository.save(new Genre(genreName))));
                    movie.getGenres().add(genre);
                }
                movieRepository.save(movie);
            }
        };
    }

    private static CatalogMovie movie(String title, int year, String language, String overview, String... genres) {
        return new CatalogMovie(title, year, language, overview, List.of(genres));
    }

    private record CatalogMovie(String title, int year, String language, String overview, List<String> genres) {
    }
}