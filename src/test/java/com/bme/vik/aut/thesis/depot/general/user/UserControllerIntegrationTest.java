package com.bme.vik.aut.thesis.depot.general.user;

import com.bme.vik.aut.thesis.depot.TestUtilities;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.auth.AuthService;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthRequest;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthResponse;
import com.bme.vik.aut.thesis.depot.security.auth.dto.RegisterRequest;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String USER_PATH = "/user";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String ADMIN_TOKEN;
    @Value("${custom.admin.username}")
    private String adminUsername;
    @Value("${custom.admin.password}")
    private String adminPassword;

    private final String userUsername = "depotuser";
    private final String userPassword = "depotpassword";

    @BeforeEach
    void setUp() {
        //***** <-- given --> *****//
        // clear default CommandLineRunner admin user from database
        userRepository.deleteAll();

        // create and save the admin user for tests
        MyUser adminUser = MyUser.builder()
                .userName(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(adminUser);


        /* Admin token is needed for acessing /user/** routes,
        so we need to authenticate the admin user and store the token.
         */
        AuthRequest authRequest = AuthRequest.builder()
                .userName(adminUsername)
                .password(adminPassword)
                .build();

        ADMIN_TOKEN = authService.authenticate(authRequest).getToken();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldNotAllowAccessToUnauthenticatedRequest() {
        //***** <-- when: Make unauthorized request to user endpoint --> *****//
        WebTestClient.ResponseSpec response = getUserInfoWithoutToken();

        //***** <-- then --> *****//
        response.expectStatus().isForbidden();
    }

    @Test
    void shouldNotAllowAccessToAuthenticatedUserWithoutSufficientPermissions() {
        //***** <-- given --> *****//
        RegisterRequest userRequest = RegisterRequest.builder()
                .userName(userUsername)
                .password(userPassword)
                .build();
        String userLvlPermissionedUserToken = authService.register(userRequest).getToken();

        //***** <-- when and then--> *****//
        getUserInfoWithToken(userLvlPermissionedUserToken)
                .expectStatus()
                .isForbidden();
    }

    @Test
    void shouldReturnAllUsers() {
        //***** <-- given: Add sample users to database --> *****//
        String userName2 = "user2";
        String userPassword2 = "password2";

        MyUser user1 = MyUser.builder().userName(userUsername).password(userPassword).role(Role.USER).build();
        MyUser user2 = MyUser.builder().userName(userName2).password(userPassword2).role(Role.USER).build();
        userRepository.saveAll(List.of(user1, user2));

        //***** <-- when & then: Retrieve all users and verify response --> *****//
        getAllUsers().hasSize(3)
                .value(users -> {
                    UserResponse adminUser = users.get(0);
                    assertEquals(adminUsername, users.get(0).getUserName());
                    assertEquals(Role.ADMIN, adminUser.getRole());
                    UserResponse responseUser1 = users.get(1);
                    assertEquals(userUsername, users.get(1).getUserName());
                    assertEquals(Role.USER, responseUser1.getRole());
                    UserResponse responseUser2 = users.get(2);
                    assertEquals(userName2, users.get(2).getUserName());
                    assertEquals(Role.USER, responseUser2.getRole());
                });
    }

    @Test
    void shouldGetUserById() {
        //***** <-- given: Save a user in database --> *****//
        MyUser user1 = MyUser.builder().userName(userUsername).password(userPassword).role(Role.USER).build();
        MyUser savedUser = userRepository.save(user1);

        //***** <-- when & then: Get user by ID and verify response --> *****//
        getUserById(savedUser.getId())
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .value(response -> {
                    assertEquals(savedUser.getId(), response.getId());
                    assertEquals(userUsername, response.getUserName());
                    assertEquals(Role.USER, response.getRole());

                    TestUtilities.assertCreatedAndUpdatedTimes(response.getCreatedAt(), response.getUpdatedAt());
                });
    }

    @Test
    void shouldNotGetUserByInvalidId() {
        //***** <-- given: Non-existing user ID --> *****//
        int invalidId = 999;

        //***** <-- when & then: Attempt to get user by invalid ID --> *****//
        getUserById(invalidId)
                .expectStatus().isNotFound()
                .expectBody(Map.class)
                .value(responseMap -> {
                    assertTrue(responseMap.containsKey("error"));
                    assertEquals("User with ID " + invalidId + " not found", responseMap.get("error"));
                });
    }

    @Test
    void shouldUpdateUserById() {
        //***** <-- given: Save a user in database --> *****//
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        MyUser user = MyUser.builder()
                .userName(userUsername)
                .password(userPassword)
                .role(Role.USER)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        MyUser savedUser = userRepository.save(user);

        String updatedUserName = "updatedUser";
        String updatedPassword = "updatedPassword";

        //***** <-- when: Update user by ID --> *****//
        updateUserById(savedUser.getId(), updatedUserName, updatedPassword)
                .expectStatus().isCreated()
                .expectBody(UserResponse.class)
                .value(response -> {
                    assertEquals(updatedUserName, response.getUserName());
                    assertEquals(Role.USER, response.getRole());

                    assertEquals(createdAt.truncatedTo(ChronoUnit.MILLIS), response.getCreatedAt().truncatedTo(ChronoUnit.MILLIS));
                    assertTrue(response.getUpdatedAt().isAfter(updatedAt));
                });
    }

    @Test
    void shouldNotUpdateUserByInvalidId() {
        //***** <-- given: Non-existing user ID and update request --> *****//
        int invalidId = 999;

        String updatedUserName = "updatedUser";
        String updatedPassword = "updatedPassword";

        //***** <-- when & then: Attempt to update user with invalid ID --> *****//
        updateUserById(invalidId, updatedUserName, updatedPassword)
                .expectStatus().isNotFound()
                .expectBody(Map.class)
                .value(responseMap -> {
                    assertTrue(responseMap.containsKey("error"));
                    assertEquals("User with ID " + invalidId + " not found", responseMap.get("error"));
                });
    }
    // todo add test for updating user with duplicated username

    @Test
    void shouldDeleteUserById() {
        //***** <-- given: Save a user in database --> *****//
        MyUser user = MyUser.builder().userName(userUsername).password(userPassword).role(Role.USER).build();
        MyUser savedUser = userRepository.save(user);

        //***** <-- when: Delete user by ID --> *****//
        deleteUserById(savedUser.getId())
                .expectStatus().isNoContent();

        //***** <-- then: Verify user is deleted from database --> *****//
        assertFalse(userRepository.findById(savedUser.getId()).isPresent());
    }

    @Test
    void shouldNotDeleteUserByInvalidId() {
        //***** <-- given: Non-existing user ID --> *****//
        int invalidId = 999;
        int numUsersBefore = userRepository.findAll().size();

        //***** <-- when & then: Attempt to delete user with invalid ID --> *****//
        deleteUserById(invalidId)
                .expectStatus().isNotFound()
                .expectBody(Map.class)
                .value(responseMap -> {
                    assertTrue(responseMap.containsKey("error"));
                    assertEquals("User with ID " + invalidId + " not found", responseMap.get("error"));
                });

        int numUsersAfter = userRepository.findAll().size();
        assertEquals(numUsersBefore, numUsersAfter);
    }

    WebTestClient.ResponseSpec getUserInfoWithoutToken() {
        return webTestClient
                .get()
                .uri(USER_PATH)
                .exchange();
    }

    WebTestClient.ResponseSpec getUserInfoWithToken(String token) {
        return webTestClient
                .get()
                .uri(USER_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .exchange();
    }

    WebTestClient.ListBodySpec<UserResponse> getAllUsers() {
        return webTestClient
                .get()
                .uri(USER_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserResponse.class);
    }

    WebTestClient.ResponseSpec getUserById(int id) {
        return webTestClient
                .get()
                .uri(USER_PATH + "/" + id)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ADMIN_TOKEN)
                .exchange();
    }

    WebTestClient.ResponseSpec updateUserById(int id, String updatedUserName, String updatedPassword) {
        UserModifyRequest updateRequest = UserModifyRequest.builder()
                .userName(updatedUserName)
                .password(updatedPassword)
                .build();

        return webTestClient
                .put()
                .uri(USER_PATH + "/" + id)
                .bodyValue(updateRequest)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ADMIN_TOKEN)
                .exchange();
    }

    WebTestClient.ResponseSpec deleteUserById(int id) {
        return webTestClient
                .delete()
                .uri(USER_PATH + "/" + id)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ADMIN_TOKEN)
                .exchange();
    }
}

