package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.TestUtilities;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.auth.AuthService;
import com.bme.vik.aut.thesis.depot.security.auth.dto.RegisterRequest;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class InfoControllerIntegrationTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String INFO_USER_ME_PATH = "/info/user/me";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String token;

    @BeforeEach
    void setUp() {
        //***** <-- given: Set up registered user and obtain token --> *****//
        String userName = "depotuser";
        String userPassword = "depotuser";
        token = registerUserAndGetToken(userName, userPassword);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldNotAllowAccessToUnauthenticatedRequest() {
        //***** <-- when: Make unauthorized request to user info endpoint --> *****//
        WebTestClient.ResponseSpec response = getUserInfoWithoutToken();

        //***** <-- then: Expect forbidden status due to lack of authentication --> *****//
        response.expectStatus().isForbidden();
    }

    @Test
    void shouldAllowAccessToAuthenticatedUserRequestAndReturnValidUserInfo() {
        //***** <-- when: Make authenticated request with valid token --> *****//
        UserResponse userResponse = getUserInfoWithToken(token);

        //***** <-- then: Validate response properties --> *****//
        assertNotNull(userResponse, "User response should not be null");
        assertEquals("depotuser", userResponse.getUserName(), "Username should match registered user");
        assertEquals(Role.USER, userResponse.getRole(), "Role should be USER");

        TestUtilities.assertCreatedAndUpdatedTimes(userResponse.getCreatedAt(), userResponse.getUpdatedAt());
    }

    private String registerUserAndGetToken(String userName, String userPassword) {
        return authService.register(RegisterRequest.builder()
                .userName(userName)
                .password(userPassword)
                .build()).getToken();
    }

    private WebTestClient.ResponseSpec getUserInfoWithoutToken() {
        return webTestClient
                .get()
                .uri(INFO_USER_ME_PATH)
                .exchange();
    }

    private UserResponse getUserInfoWithToken(String token) {
        return webTestClient
                .get()
                .uri(INFO_USER_ME_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();
    }
}

