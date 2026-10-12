package com.netflixai.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String overview;

    private LocalDate releaseDate;
    private Integer releaseYear;
    private Integer runtime;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private BigDecimal rating;

    @Size(max = 500)
    private String posterUrl;

    @Size(max = 500)
    private String backdropUrl;

    @Size(max = 500)
    private String trailerUrl;

    @Size(max = 20)
    private String language;

    @Size(max = 100)
    private String country;

    private Boolean featured = false;

        private Boolean demoContent = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_cast",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "cast_member_id")
    )
    private Set<CastMember> castMembers = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_directors",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "director_id")
    )
    private Set<Director> directors = new HashSet<>();

    protected Movie() {
    }

    public Movie(String title, String overview, LocalDate releaseDate, Integer releaseYear, Integer runtime,
                 BigDecimal rating, String posterUrl, String backdropUrl, String trailerUrl, String language,
                 String country, Boolean featured) {
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.releaseYear = releaseYear;
        this.runtime = runtime;
        this.rating = rating;
        this.posterUrl = posterUrl;
        this.backdropUrl = backdropUrl;
        this.trailerUrl = trailerUrl;
        this.language = language;
        this.country = country;
        this.featured = featured != null && featured;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getOverview() { return overview; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public Integer getReleaseYear() { return releaseYear; }
    public Integer getRuntime() { return runtime; }
    public BigDecimal getRating() { return rating; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getBackdropUrl() { return backdropUrl; }
    public String getTrailerUrl() { return trailerUrl; }
    public String getLanguage() { return language; }
    public String getCountry() { return country; }
    public Boolean getFeatured() { return featured; }
        public Boolean getDemoContent() { return demoContent; }
        public void setDemoContent(Boolean demoContent) { this.demoContent = demoContent != null && demoContent; }
    public Set<Genre> getGenres() { return genres; }
    public Set<CastMember> getCastMembers() { return castMembers; }
    public Set<Director> getDirectors() { return directors; }

    public void updateDetails(String title, String overview, LocalDate releaseDate, Integer releaseYear,
                              Integer runtime, BigDecimal rating, String posterUrl, String backdropUrl,
                              String trailerUrl, String language, String country, Boolean featured) {
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.releaseYear = releaseYear;
        this.runtime = runtime;
        this.rating = rating;
        this.posterUrl = posterUrl;
        this.backdropUrl = backdropUrl;
        this.trailerUrl = trailerUrl;
        this.language = language;
        this.country = country;
        this.featured = featured != null && featured;
    }
}
