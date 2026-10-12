package com.netflixai.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.netflixai.backend.dto.RegisterRequest;
import com.netflixai.backend.dto.UserResponse;
import com.netflixai.backend.entity.AppUser;
import com.netflixai.backend.repository.AppUserRepository;

@Service
public class AuthenticationService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be 72 bytes or fewer");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        AppUser user = new AppUser(request.fullName().trim(), email, passwordEncoder.encode(request.password()));
        try {
            return toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserResponse(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(AuthenticationService::toResponse)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }

    public static class DuplicateEmailException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
