package com.bme.vik.aut.thesis.depot.general.supplier.supplier;

import static org.junit.jupiter.api.Assertions.*;
import com.bme.vik.aut.thesis.depot.general.order.OrderRepository;
import com.bme.vik.aut.thesis.depot.general.report.dto.InventoryState;
import com.bme.vik.aut.thesis.depot.general.report.dto.ProductState;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierService;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.SupplierCreationResponse;
import com.bme.vik.aut.thesis.depot.general.util.*;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.order.Order;
import com.bme.vik.aut.thesis.depot.general.supplier.product.*;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.auth.AuthService;
import com.bme.vik.aut.thesis.depot.security.jwt.JwtTokenService;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class SupplierStockControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductSchemaRepository productSchemaRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String ADMIN_TOKEN;
    private String SUPPLIER_TOKEN;
    @Value("${custom.admin.username}")
    private String adminUsername;
    @Value("${custom.admin.password}")
    private String adminPassword;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ADMIN_TOKEN = TestUtil.createAndRegisterUser(
                userRepository,
                adminUsername,
                adminPassword,
                Role.ADMIN,
                authService,
                passwordEncoder
        );

        SupplierCreationResponse supplierResponse = TestUtil.createSupplierWithAPI(
                webTestClient,
                ADMIN_TOKEN,
                "Test Supplier",
                10,
                5,
                15,
                50
        );
        SUPPLIER_TOKEN = supplierResponse.getToken();
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
        productSchemaRepository.deleteAll();
        inventoryRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldFetchInventorySuccessfully() {
        //***** <-- given: Create category and product schema --> *****//
        Category category = TestUtil.createCategoryWithAPI(
                webTestClient,
                ADMIN_TOKEN,
                "Test Category",
                "Category Description"
        );

        ProductSchema productSchema = TestUtil.createProductSchemaWithAPI(
                webTestClient,
                ADMIN_TOKEN,
                "Test Product",
                10,
                List.of(category)
        );

        //***** <-- given: Add stock to the supplier's inventory --> *****//
        TestUtil.addStockToSupplierInventoryWithAPI(
                webTestClient,
                SUPPLIER_TOKEN,
                productSchema,
                "Sample Product Description",
                5,
                LocalDateTime.now().plusDays(10)
        );

        //***** <-- when: Fetch inventory --> *****//
        webTestClient.get()
                .uri("/supplier/inventory")
                .header("Authorization", "Bearer " + SUPPLIER_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(InventoryState.class)
                .value(inventoryState -> {
                    assertNotNull(inventoryState);
                    assertEquals(1000, inventoryState.getMaxAvailableSpace());
                    assertEquals(50, inventoryState.getUsedSpace());
                    assertEquals(950, inventoryState.getMaxAvailableSpace() - inventoryState.getUsedSpace());
                    assertNotNull(inventoryState.getProductStats());
                    assertTrue(inventoryState.getProductStats().containsKey(productSchema.getId()));
                    assertEquals(5L, inventoryState.getProductStats().get(productSchema.getId()).get(ProductStatus.FREE));
                    assertNotNull(inventoryState.getStock());
                    assertTrue(inventoryState.getStock().containsKey(productSchema.getId()));
                    List<ProductState> productStates = inventoryState.getStock().get(productSchema.getId());
                    assertNotNull(productStates);
                    assertEquals(5, productStates.size());
                    ProductState product = productStates.get(0);
                    assertNotNull(product);
                    assertEquals(productSchema.getId(), product.getProductSchemaID());
                    assertEquals("Test Product", product.getProductName());
                    assertEquals(ProductStatus.FREE, product.getStatus());
                    assertEquals(10, product.getStorageSpaceNeeded());
                    assertNotNull(product.getCategoryIDs());
                    assertTrue(product.getCategoryIDs().contains(category.getId()));
                    assertNotNull(product.getCreatedAt());
                    assertNotNull(product.getUpdatedAt());
                });
    }

    @Test
    void shouldAddStockSuccessfully() {
        //***** <-- given: Create category and product schema --> *****//
        Category category = TestUtil.createCategoryWithAPI(
                webTestClient,
                ADMIN_TOKEN,
                "Test Category",
                "Category Description"
        );

        ProductSchema productSchema = TestUtil.createProductSchemaWithAPI(
                webTestClient,
                ADMIN_TOKEN,
                "Test Product",
                5,
                List.of(category)
        );

        //***** <-- when: Add stock --> *****//
        TestUtil.addStockToSupplierInventoryWithAPI(
                webTestClient,
                SUPPLIER_TOKEN,
                productSchema,
                "Sample Product Description",
                10,
                LocalDateTime.now().plusDays(10)
        );

        //***** <-- then: Verify stock addition --> *****//
        Inventory inventory = inventoryRepository.findAll().get(0);
        assertEquals(10, inventory.getProductIds().size());
        assertEquals(50, inventory.getUsedSpace());
        assertEquals(950, inventory.getMaxAvailableSpace() - inventory.getUsedSpace());

        List<Product> products = productRepository.findAll();
        assertEquals(10, products.size());
        assertTrue(products.stream().allMatch(p -> p.getStatus() == ProductStatus.FREE));
        assertTrue(products.stream().allMatch(p -> p.getSchema().getId().equals(productSchema.getId())));
    }

    @Test
    void shouldRemoveStockSuccessfully() {
        //***** <-- given: Create category, product schema, and add stock --> *****//
        Category category = TestUtil.createCategoryWithAPI(
                webTestClient,
                ADMIN_TOKEN,
                "Test Category",
                "Category Description"
        );

        ProductSchema productSchema = TestUtil.createProductSchemaWithAPI(
                webTestClient,
                ADMIN_TOKEN,
                "Test Product",
                5,
                List.of(category)
        );

        TestUtil.addStockToSupplierInventoryWithAPI(
                webTestClient,
                SUPPLIER_TOKEN,
                productSchema,
                "Sample Product Description",
                10,
                LocalDateTime.now().plusDays(10)
        );

        //***** <-- when: Remove stock --> *****//
        TestUtil.removeStockFromSupplierInventory(
                webTestClient,
                SUPPLIER_TOKEN,
                productSchema,
                5
        );

        //***** <-- then: Verify stock removal --> *****//
        Inventory inventory = inventoryRepository.findAll().get(0);
        assertEquals(5, inventory.getProductIds().size());
        assertEquals(25, inventory.getUsedSpace());
        assertEquals(975, inventory.getMaxAvailableSpace() - inventory.getUsedSpace());

        List<Product> products = productRepository.findAll();
        assertEquals(5, products.size());
        assertTrue(products.stream().allMatch(p -> p.getStatus() == ProductStatus.FREE));
        assertTrue(products.stream().allMatch(p -> p.getSchema().getId().equals(productSchema.getId())));
    }

    @Test
    void shouldFetchAllProductsSuccessfully() {
        //***** <-- given: Create category, product schema, and add stock --> *****//
        Category category = TestUtil.createCategoryWithAPI(
                webTestClient,
                ADMIN_TOKEN,
                "Test Category",
                "Category Description"
        );

        ProductSchema productSchema = TestUtil.createProductSchemaWithAPI(
                webTestClient,
                ADMIN_TOKEN,
                "Test Product",
                5,
                List.of(category)
        );

        TestUtil.addStockToSupplierInventoryWithAPI(
                webTestClient,
                SUPPLIER_TOKEN,
                productSchema,
                "Sample Product Description",
                10,
                LocalDateTime.now().plusDays(10)
        );

        //***** <-- when: Fetch all products --> *****//
        webTestClient.get()
                .uri("/supplier/product")
                .header("Authorization", "Bearer " + SUPPLIER_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Product.class)
                .value(products -> {
                    assertEquals(10, products.size());
                    assertTrue(products.stream().allMatch(p -> p.getSchema().getId().equals(productSchema.getId())));
                });
    }
}
