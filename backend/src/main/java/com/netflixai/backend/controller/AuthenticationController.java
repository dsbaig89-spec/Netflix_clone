package com.netflixai.backend.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflixai.backend.dto.CsrfResponse;
import com.netflixai.backend.dto.LoginRequest;
import com.netflixai.backend.dto.RegisterRequest;
import com.netflixai.backend.dto.UserResponse;
import com.netflixai.backend.exception.ApiErrorResponse;
import com.netflixai.backend.service.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthenticationController {

    private static final int REMEMBERED_SESSION_SECONDS = (int) Duration.ofDays(30).toSeconds();

    private final AuthenticationService authenticationService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final boolean sessionCookieSecure;
    private final String sessionCookieSameSite;

    public AuthenticationController(AuthenticationService authenticationService,
                                    AuthenticationManager authenticationManager,
                                    SecurityContextRepository securityContextRepository,
                                    @Value("${server.servlet.session.cookie.secure:false}") boolean sessionCookieSecure,
                                    @Value("${server.servlet.session.cookie.same-site:Lax}") String sessionCookieSameSite) {
        this.authenticationService = authenticationService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionCookieSecure = sessionCookieSecure;
        this.sessionCookieSameSite = sessionCookieSameSite;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.register(request));
        } catch (AuthenticationService.DuplicateEmailException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiErrorResponse(409, "EMAIL_ALREADY_EXISTS",
                            "An account with this email already exists"));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponse(400, "INVALID_REQUEST", exception.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            AuthenticationService.normalizeEmail(request.email()), request.password()));
        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse(401, "INVALID_CREDENTIALS", "Email or password is incorrect"));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse(401, "INVALID_CREDENTIALS", "Email or password is incorrect"));
        }

        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        if (request.rememberMe()) {
            httpRequest.getSession(true).setMaxInactiveInterval(REMEMBERED_SESSION_SECONDS);
            ResponseCookie sessionCookie = ResponseCookie.from("STREAMFLIX_SESSION",
                            httpRequest.getSession().getId())
                    .httpOnly(true)
                    .secure(sessionCookieSecure)
                    .sameSite(sessionCookieSameSite)
                    .path("/")
                    .maxAge(Duration.ofDays(30))
                    .build();
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return ResponseEntity.ok(authenticationService.getUserResponse(authentication.getName()));
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return authenticationService.getUserResponse(authentication.getName());
    }
}
