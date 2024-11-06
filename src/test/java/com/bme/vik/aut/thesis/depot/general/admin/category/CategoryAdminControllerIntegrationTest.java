package com.bme.vik.aut.thesis.depot.general.admin.category;

import com.bme.vik.aut.thesis.depot.TestUtilities;
import com.bme.vik.aut.thesis.depot.general.admin.category.dto.CreateCategoryRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.auth.AuthService;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class CategoryAdminControllerIntegrationTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String CATEGORY_PATH = "/admin/category";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthService authService;

    private String adminToken;

    @Value("${custom.admin.username}")
    private String adminUsername;

    @Value("${custom.admin.password}")
    private String adminPassword;

    @BeforeEach
    void setUp() {
        adminToken = authService.authenticate(
                AuthRequest.builder().userName(adminUsername).password(adminPassword).build()).getToken();
    }

    @AfterEach
    void tearDown() {
        categoryRepository.deleteAll();
    }

    @Test
    void shouldNotAllowAccessToUnauthenticatedRequest() {
        getCategoryInfoWithoutToken()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldNotAllowAccessToAuthenticatedUserWithoutSufficientPermissions() {
        String userToken = authService.register(
                RegisterRequest.builder().userName("user").password("password").build()).getToken();

        getCategoryInfoWithToken(userToken)
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturnAllCategories() {
        Category category1 = Category.builder().name("Category1").description("Description1").build();
        Category category2 = Category.builder().name("Category2").description("Description2").build();
        categoryRepository.saveAll(List.of(category1, category2));

        getAllCategories()
                .expectStatus().isOk()
                .expectBodyList(Category.class)
                .hasSize(2)
                .value(categories -> {
                    assertEquals("Category1", categories.get(0).getName());
                    assertEquals("Category2", categories.get(1).getName());
                });
    }

    @Test
    void shouldGetCategoryById() {
        Category category = categoryRepository.save(Category.builder().name("Category").description("Description").build());

        getCategoryById(category.getId())
                .expectStatus().isOk()
                .expectBody(Category.class)
                .value(response -> {
                    assertEquals("Category", response.getName());
                    assertEquals("Description", response.getDescription());
                    TestUtilities.assertCreatedAndUpdatedTimes(response.getCreatedAt(), response.getUpdatedAt());
                });
    }

    @Test
    void shouldNotGetCategoryByInvalidId() {
        Long invalidId = 999L;

        getCategoryById(invalidId)
                .expectStatus().isNotFound()
                .expectBody(Map.class)
                .value(response -> {
                    assertTrue(response.containsKey("error"));
                    assertEquals("Category with ID " + invalidId + " not found", response.get("error"));
                });
    }

    @Test
    void shouldCreateCategory() {
        createCategory("NewCategory", "Description")
                .expectStatus().isCreated()
                .expectBody(Category.class)
                .value(response -> {
                    assertEquals("NewCategory", response.getName());
                    assertEquals("Description", response.getDescription());
                    TestUtilities.assertCreatedAndUpdatedTimes(response.getCreatedAt(), response.getUpdatedAt());
                });
    }

    @Test
    void shouldNotCreateCategoryWithDuplicatedName() {
        categoryRepository.save(Category.builder().name("ExistingCategory").description("Description").build());

        createCategory("ExistingCategory", "New Description")
                .expectStatus().is4xxClientError()
                .expectBody(Map.class)
                .value(response -> {
                    assertTrue(response.containsKey("error"));
                    assertEquals("Category with name ExistingCategory already exists", response.get("error"));
                });
    }

    @Test
    void shouldUpdateCategoryById() {
        Category category = categoryRepository.save(Category.builder().name("OldName").description("OldDescription").build());

        LocalDateTime updatedAt = category.getUpdatedAt();

        updateCategoryById(category.getId(), "UpdatedName", "UpdatedDescription")
                .expectStatus().isOk()
                .expectBody(Category.class)
                .value(response -> {
                    assertEquals("UpdatedName", response.getName());
                    assertEquals("UpdatedDescription", response.getDescription());

                    assertTrue(response.getUpdatedAt().isAfter(updatedAt));
                    TestUtilities.assertCreatedAndUpdatedTimes(response.getCreatedAt(), response.getUpdatedAt());
                });
    }

    @Test
    void shouldNotUpdateCategoryByInvalidId() {
        Long invalidId = 999L;

        updateCategoryById(invalidId, "UpdatedName", "UpdatedDescription")
                .expectStatus().isNotFound()
                .expectBody(Map.class)
                .value(response -> {
                    assertTrue(response.containsKey("error"));
                    assertEquals("Category with ID " + invalidId + " not found", response.get("error"));
                });
    }

    @Test
    void shouldDeleteCategoryById() {
        Category category = categoryRepository.save(Category.builder().name("Category").description("Description").build());

        deleteCategoryById(category.getId())
                .expectStatus().isNoContent();

        assertFalse(categoryRepository.findById(category.getId()).isPresent());
    }

    @Test
    void shouldNotDeleteCategoryByInvalidId() {
        Long invalidId = 999L;

        deleteCategoryById(invalidId)
                .expectStatus().isNotFound()
                .expectBody(Map.class)
                .value(response -> {
                    assertTrue(response.containsKey("error"));
                    assertEquals("Category with ID " + invalidId + " not found", response.get("error"));
                });
    }

    WebTestClient.ResponseSpec getCategoryInfoWithoutToken() {
        return webTestClient
                .get()
                .uri(CATEGORY_PATH)
                .exchange();
    }

    WebTestClient.ResponseSpec getCategoryInfoWithToken(String token) {
        return webTestClient
                .get()
                .uri(CATEGORY_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .exchange();
    }

    WebTestClient.ResponseSpec getAllCategories() {
        return webTestClient.get().uri(CATEGORY_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .exchange();
    }

    WebTestClient.ResponseSpec getCategoryById(Long id) {
        return webTestClient
                .get()
                .uri(CATEGORY_PATH + "/" + id)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .exchange();
    }

    WebTestClient.ResponseSpec createCategory(String categoryName, String description) {
        CreateCategoryRequest request = CreateCategoryRequest.builder().name(categoryName).description(description).build();

        return webTestClient
                .post()
                .uri(CATEGORY_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .bodyValue(request)
                .exchange();
    }

    WebTestClient.ResponseSpec updateCategoryById(Long id, String categoryName, String description) {
        CreateCategoryRequest updateRequest = CreateCategoryRequest.builder().name(categoryName).description(description).build();

        return webTestClient
                .put()
                .uri(CATEGORY_PATH + "/" + id)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .bodyValue(updateRequest)
                .exchange();
    }

    WebTestClient.ResponseSpec deleteCategoryById(Long id) {
        return webTestClient
                .delete()
                .uri(CATEGORY_PATH + "/" + id)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .exchange();
    }

}
