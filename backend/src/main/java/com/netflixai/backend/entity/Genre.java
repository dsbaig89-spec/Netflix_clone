package com.netflixai.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "genres", uniqueConstraints = @UniqueConstraint(name = "uk_genres_name", columnNames = "name"))
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 80)
    private String name;

    protected Genre() {
    }

    public Genre(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
