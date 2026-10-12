package com.netflixai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.netflixai.backend.entity.Director;

public interface DirectorRepository extends JpaRepository<Director, Long> {
	Optional<Director> findByNameIgnoreCase(String name);
}
