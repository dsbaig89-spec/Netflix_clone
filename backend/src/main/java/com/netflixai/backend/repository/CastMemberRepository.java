package com.netflixai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.netflixai.backend.entity.CastMember;

public interface CastMemberRepository extends JpaRepository<CastMember, Long> {
	Optional<CastMember> findByNameIgnoreCase(String name);
}
