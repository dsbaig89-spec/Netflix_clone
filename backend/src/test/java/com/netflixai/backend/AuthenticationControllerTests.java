package com.netflixai.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.netflixai.backend.repository.AppUserRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void registrationStoresOnlyTheBcryptHashAndReturnsSafeUserData() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Stream User","email":"User@Example.com",
                                 "password":"StrongPass1!","confirmPassword":"StrongPass1!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Stream User"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        var savedUser = userRepository.findByEmail("user@example.com").orElseThrow();
        assertNotNull(savedUser.getCreatedAt());
        assertTrue(savedUser.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches("StrongPass1!", savedUser.getPasswordHash()));
    }

    @Test
    void duplicateEmailIsRejectedWithoutCreatingAnotherUser() throws Exception {
        register("person@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Another Name","email":"PERSON@example.com",
                                 "password":"StrongPass1!","confirmPassword":"StrongPass1!"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));

        org.junit.jupiter.api.Assertions.assertEquals(1, userRepository.count());
    }

    @Test
    void invalidEmailMissingFieldsAndMismatchedPasswordsAreRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("""
                                {"fullName":"User","email":"not-an-email",
                                 "password":"StrongPass1!","confirmPassword":"StrongPass1!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("""
                                {"fullName":"User","email":"user@example.com",
                                 "password":"StrongPass1!","confirmPassword":"Different1!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    void stateChangingRequestsRequireCsrfAndCsrfEndpointProvidesToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"fullName":"User","email":"user@example.com",
                                 "password":"StrongPass1!","confirmPassword":"StrongPass1!"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void corsAllowsCredentialedRequestsFromTheViteOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsAllowsCredentialedRequestsFromTheAlternateVitePort() throws Exception {
        mockMvc.perform(options("/api/auth/me")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void loginCreatesSessionAndReturnsCurrentUserAfterRefresh() throws Exception {
        register("member@example.com");

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("""
                                {"email":"MEMBER@example.com","password":"StrongPass1!","rememberMe":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void invalidCredentialsAndProtectedRoutesAreRejected() throws Exception {
        register("member@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("""
                                {"email":"member@example.com","password":"Incorrect1!","rememberMe":false}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/private"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesTheAuthenticatedSession() throws Exception {
        register("member@example.com");
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("""
                                {"email":"member@example.com","password":"StrongPass1!","rememberMe":false}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf().asHeader()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rememberMeUsesPersistentHttpOnlyCookie() throws Exception {
        register("remember@example.com");
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("""
                                {"email":"remember@example.com","password":"StrongPass1!","rememberMe":true}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var cookies = login.getResponse().getHeaders("Set-Cookie");
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("Max-Age=2592000")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("HttpOnly")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains("SameSite=Lax")));
        assertFalse(cookies.stream().anyMatch(cookie -> cookie.contains("password")));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("""
                        {"fullName":"Test User","email":"%s",
                         "password":"StrongPass1!","confirmPassword":"StrongPass1!"}
                        """.formatted(email)))
                .andExpect(status().isCreated());
    }
}
