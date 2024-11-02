package com.bme.vik.aut.thesis.depot.security.auth;

import com.bme.vik.aut.thesis.depot.TestUtilities;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthRequest;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthResponse;
import com.bme.vik.aut.thesis.depot.security.auth.dto.RegisterRequest;
import com.bme.vik.aut.thesis.depot.security.config.SecurityConfig;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtAuthFilter;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtTokenService;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    private static final String AUTH_REGISTER_PATH = "/auth/register";
    private static final String AUTH_AUTHENTICATE_PATH = "/auth/authenticate";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldSuccessfullyRegisterNewUser() {
        //***** <-- given --> *****//
        String userName = "depotuser";
        String userPassword = "depotuser";

        //***** <-- when --> *****//
        AuthResponse authResponse = registerUserForReal(userName, userPassword);

        //***** <-- then --> *****//
        assertNotNull(authResponse);
        assertNotNull(authResponse.getToken());

        // Validate the saved user in the database
        Optional<MyUser> newUser = userRepository.findByUserName(userName);
        assertTrue(newUser.isPresent());
        assertEquals(userName, newUser.get().getUsername());
        assertEquals(Role.USER, newUser.get().getRole());

        // Validate created and updated timestamps
        TestUtilities.assertCreatedAndUpdatedTimes(newUser.get().getCreatedAt(), newUser.get().getUpdatedAt());

        // Validate token
        assertTrue(jwtTokenService.isTokenValid(authResponse.getToken(), newUser.get()));
        assertEquals(jwtTokenService.extractUsername(authResponse.getToken()), userName);
    }

    @Test
    void shouldNotRegisterNewUserWithSameUserName() {
        //***** <-- given --> *****//
        String userName = "duplicateuser";
        String userPassword = "password";

        // Register the first user
        registerUserForReal(userName, userPassword);

        //***** <-- when & then: Attempt to register the same username --> *****//
        tryToRegisterUser(userName, userPassword).expectStatus().is4xxClientError();

        assertEquals(1, userRepository.findByUserName(userName).stream().count());
    }

    @Test
    void shouldSuccessfullyAuthenticateAlreadyRegisteredUser() {
        //***** <-- given: Register user --> *****//
        String userName = "existinguser";
        String userPassword = "password";
        registerUserForReal(userName, userPassword);

        //***** <-- when: Authenticate registered user --> *****//
        AuthResponse authResponse = authenticateUserForReal(userName, userPassword);

        //***** <-- then: Validate response token --> *****//
        assertNotNull(authResponse);
        assertNotNull(authResponse.getToken());

        // Validate token content
        Optional<MyUser> user = userRepository.findByUserName(userName);
        assertTrue(user.isPresent());
        assertTrue(jwtTokenService.isTokenValid(authResponse.getToken(), user.get()));
        assertEquals(jwtTokenService.extractUsername(authResponse.getToken()), userName);
    }

    @Test
    void shouldNotAuthenticateUnRegisteredUser() {
        //***** <-- given: Non-existing user credentials --> *****//
        String userName = "nonexistentuser";
        String userPassword = "password";

        //***** <-- when & then: Attempt to authenticate non-registered user --> *****//
        tryToAuthenticateUser(userName, userPassword).expectStatus().isUnauthorized();

        // Validate that the user is not saved in the database
        assertFalse(userRepository.findByUserName(userName).isPresent());
    }

    // Helper methods
    private WebTestClient.ResponseSpec tryToRegisterUser(String userName, String userPassword) {
        RegisterRequest registerRequest = new RegisterRequest(userName, userPassword);
        return webTestClient
                .post()
                .uri(AUTH_REGISTER_PATH)
                .bodyValue(registerRequest)
                .exchange();
    }

    private AuthResponse registerUserForReal(String userName, String userPassword) {
        return tryToRegisterUser(userName, userPassword)
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private WebTestClient.ResponseSpec tryToAuthenticateUser(String userName, String userPassword) {
        AuthRequest authRequest = new AuthRequest(userName, userPassword);
        return webTestClient
                .post()
                .uri(AUTH_AUTHENTICATE_PATH)
                .bodyValue(authRequest)
                .exchange();
    }

    private AuthResponse authenticateUserForReal(String userName, String userPassword) {
        return tryToAuthenticateUser(userName, userPassword)
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
    }

}