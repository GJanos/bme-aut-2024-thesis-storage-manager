package com.bme.vik.aut.thesis.depot.general.admin.productschema;

import com.bme.vik.aut.thesis.depot.TestUtilities;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.category.dto.CreateCategoryRequest;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.dto.CreateProductSchemaRequest;
import com.bme.vik.aut.thesis.depot.security.auth.AuthService;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthRequest;
import com.bme.vik.aut.thesis.depot.security.auth.dto.RegisterRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class ProductSchemaAdminControllerIntegrationTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PRODUCT_SCHEMA_PATH = "/admin/product-schema";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProductSchemaRepository productSchemaRepository;

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
        productSchemaRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void shouldNotAllowAccessToUnauthenticatedRequest() {
        getPSInfoWithoutToken()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldNotAllowAccessToAuthenticatedUserWithoutSufficientPermissions() {
        String userToken = authService.register(
                RegisterRequest.builder().userName("user").password("password").build()).getToken();

        getPSInfoWithToken(userToken)
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturnAllPS() {
        Category category1 = categoryRepository.save(Category.builder().name("Category1").build());
        Category category2 = categoryRepository.save(Category.builder().name("Category2").build());

        ProductSchema productSchema1 = ProductSchema.builder()
                .name("Product1")
                .storageSpaceNeeded(10)
                .categories(List.of(category1))
                .build();

        ProductSchema productSchema2 = ProductSchema.builder()
                .name("Product2")
                .storageSpaceNeeded(15)
                .categories(List.of(category2))
                .build();

        productSchemaRepository.saveAll(List.of(productSchema1, productSchema2));

        getAllPS()
                .expectStatus().isOk()
                .expectBodyList(ProductSchema.class)
                .hasSize(2)
                .value(productSchemas -> {
                    ProductSchema ps1 = productSchemas.get(0);
                    ProductSchema ps2 = productSchemas.get(1);

                    assertEquals("Product1", ps1.getName());
                    assertEquals(10, ps1.getStorageSpaceNeeded());
                    assertEquals(1, ps1.getCategories().size());
                    assertEquals("Category1", ps1.getCategories().get(0).getName());

                    assertEquals("Product2", ps2.getName());
                    assertEquals(15, ps2.getStorageSpaceNeeded());
                    assertEquals(1, ps2.getCategories().size());
                    assertEquals("Category2", ps2.getCategories().get(0).getName());
                });
    }


    @Test
    void shouldGetPSById() {
        ProductSchema productSchema = productSchemaRepository.save(ProductSchema.builder().name("Product").storageSpaceNeeded(10).build());

        getPSById(productSchema.getId())
                .expectStatus().isOk()
                .expectBody(ProductSchema.class)
                .value(response -> {
                    assertEquals("Product", response.getName());
                    assertEquals(10, response.getStorageSpaceNeeded());
                });
    }

    @Test
    void shouldNotGetPSByInvalidId() {
        Long invalidId = 999L;

        getPSById(invalidId)
                .expectStatus().isNotFound()
                .expectBody(Map.class)
                .value(response -> {
                    assertTrue(response.containsKey("error"));
                    assertEquals("Product schema with ID " + invalidId + " not found", response.get("error"));
                });
    }

    @Test
    void shouldCreatePS() {
        Category category1 = categoryRepository.save(
                Category.builder().name("Category1").description("Description1").build());

        Category category2 = categoryRepository.save(
                Category.builder().name("Category2").description("Description2").build());

        // Use the actual IDs assigned by the database
        createPS("NewProduct", 20, List.of(category1.getId(), category2.getId()))
                .expectStatus().isCreated()
                .expectBody(ProductSchema.class)
                .value(response -> {
                    assertEquals("NewProduct", response.getName());
                    assertEquals(20, response.getStorageSpaceNeeded());
                    assertEquals(2, response.getCategories().size());
                    assertEquals("Category1", response.getCategories().get(0).getName());
                });
    }


    @Test
    void shouldNotCreatePSWithDuplicatedName() {
        ProductSchema productSchema = productSchemaRepository.save(ProductSchema.builder().name("ExistingProduct").storageSpaceNeeded(10).build());

        createPS("ExistingProduct", 15, List.of(productSchema.getId()))
                .expectStatus().is4xxClientError()
                .expectBody(Map.class)
                .value(response -> {
                    assertTrue(response.containsKey("error"));
                    assertEquals("Product schema with name ExistingProduct already exists", response.get("error"));
                });
    }

    @Test
    void shouldUpdatePSById() {
        ProductSchema productSchema = productSchemaRepository.save(ProductSchema.builder().name("OldName").storageSpaceNeeded(10).build());

        updatePSById(productSchema.getId(), "UpdatedName", 15, List.of())
                .expectStatus().isOk()
                .expectBody(ProductSchema.class)
                .value(response -> {
                    assertEquals("UpdatedName", response.getName());
                    assertEquals(15, response.getStorageSpaceNeeded());
                    assertEquals(0, response.getCategories().size());
                });
    }

    @Test
    void shouldNotUpdatePSByInvalidId() {
        Long invalidId = 999L;

        updatePSById(invalidId, "UpdatedName", 15, List.of())
                .expectStatus().isNotFound()
                .expectBody(Map.class)
                .value(response -> {
                    assertTrue(response.containsKey("error"));
                    assertEquals("Product schema with ID " + invalidId + " not found", response.get("error"));
                });
    }

    @Test
    void shouldDeleteCategoryById() {
        ProductSchema productSchema = productSchemaRepository.save(ProductSchema.builder().name("ProductToDelete").storageSpaceNeeded(10).build());

        deletePSById(productSchema.getId())
                .expectStatus().isNoContent();

        assertFalse(productSchemaRepository.findById(productSchema.getId()).isPresent());
    }

    @Test
    void shouldNotDeletePSByInvalidId() {
        Long invalidId = 999L;

        deletePSById(invalidId)
                .expectStatus().isNotFound()
                .expectBody(Map.class)
                .value(response -> {
                    assertTrue(response.containsKey("error"));
                    assertEquals("Product schema with ID " + invalidId + " not found", response.get("error"));
                });
    }

    WebTestClient.ResponseSpec getPSInfoWithoutToken() {
        return webTestClient.get().uri(PRODUCT_SCHEMA_PATH).exchange();
    }

    WebTestClient.ResponseSpec getPSInfoWithToken(String token) {
        return webTestClient.get().uri(PRODUCT_SCHEMA_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token).exchange();
    }

    WebTestClient.ResponseSpec getAllPS() {
        return webTestClient.get().uri(PRODUCT_SCHEMA_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken).exchange();
    }

    WebTestClient.ResponseSpec getPSById(Long id) {
        return webTestClient.get().uri(PRODUCT_SCHEMA_PATH + "/" + id)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken).exchange();
    }

    WebTestClient.ResponseSpec createPS(String name, int storageSpaceNeeded, List<Long> categoryIDs) {
        CreateProductSchemaRequest request = CreateProductSchemaRequest.builder()
                .name(name)
                .storageSpaceNeeded(storageSpaceNeeded)
                .categoryIDs(categoryIDs)
                .build();

        return webTestClient.post().uri(PRODUCT_SCHEMA_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .bodyValue(request).exchange();
    }

    WebTestClient.ResponseSpec updatePSById(Long id, String name, int storageSpaceNeeded, List<Long> categoryIDs) {
        CreateProductSchemaRequest request = CreateProductSchemaRequest.builder()
                .name(name)
                .storageSpaceNeeded(storageSpaceNeeded)
                .categoryIDs(categoryIDs)
                .build();

        return webTestClient.put().uri(PRODUCT_SCHEMA_PATH + "/" + id)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .bodyValue(request).exchange();
    }

    WebTestClient.ResponseSpec deletePSById(Long id) {
        return webTestClient.delete().uri(PRODUCT_SCHEMA_PATH + "/" + id)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken).exchange();
    }
}
