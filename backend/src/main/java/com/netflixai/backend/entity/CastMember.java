package com.netflixai.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "cast_members")
public class CastMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 150)
    private String characterName;

    protected CastMember() {
    }

    public CastMember(String name, String characterName) {
        this.name = name;
        this.characterName = characterName;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCharacterName() { return characterName; }
}
