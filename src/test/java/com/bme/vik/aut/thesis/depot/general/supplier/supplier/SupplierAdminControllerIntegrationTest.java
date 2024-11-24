package com.bme.vik.aut.thesis.depot.general.supplier.supplier;

import com.bme.vik.aut.thesis.depot.general.order.OrderRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierService;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.CreateSupplierRequest;
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

import static org.junit.jupiter.api.Assertions.*;
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
class SupplierAdminControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String ADMIN_TOKEN;
    @Value("${custom.admin.username}")
    private String adminUsername;
    @Value("${custom.admin.password}")
    private String adminPassword;

    @Autowired
    private SupplierService supplierService;
    @Autowired
    private AuthService authService;
    @Autowired
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(supplierService, "SHOULD_GENERATE_RANDOM_PASSWORD", true);

        ADMIN_TOKEN = TestUtil.createAndRegisterUser(
                userRepository,
                adminUsername,
                adminPassword,
                Role.ADMIN,
                authService,
                passwordEncoder);

    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        inventoryService.clearStock();
    }

    @Test
    void shouldFetchAllSuppliersSuccessfully() {
        //***** <-- given: Create suppliers --> *****//
        TestUtil.createSupplier(
                userRepository,
                10,
                5,
                15,
                50,
                "Supplier A",
                "passwordA",
                passwordEncoder
        );
        TestUtil.createSupplier(
                userRepository,
                20,
                10,
                25,
                60,
                "Supplier B",
                "passwordB",
                passwordEncoder
        );

        //***** <-- when: Fetch all suppliers --> *****//
        webTestClient.get()
                .uri("/admin/supplier")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Supplier.class)
                .value(suppliers -> {
                    assertEquals(2, suppliers.size());
                    assertTrue(suppliers.stream().anyMatch(s -> s.getName().equals("Supplier A")));
                    assertTrue(suppliers.stream().anyMatch(s -> s.getName().equals("Supplier B")));
                    // todo little more assertions
                });
    }

    @Test
    void shouldFetchSupplierByIdSuccessfully() {
        //***** <-- given: Create a supplier --> *****//
        Supplier supplier = TestUtil.createSupplier(
                userRepository,
                10,
                5,
                15,
                50,
                "Supplier A",
                "passwordA",
                passwordEncoder
        ).supplier();

        //***** <-- when: Fetch supplier by ID --> *****//
        webTestClient.get()
                .uri("/admin/supplier/" + supplier.getId())
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Supplier.class)
                .value(responseSupplier -> {
                    assertEquals(supplier.getId(), responseSupplier.getId());
                    assertEquals("Supplier A", responseSupplier.getName());
                });
    }

    @Test
    void shouldCreateSupplierSuccessfully() {
        //***** <-- given: Create supplier request --> *****//
        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .name("New Supplier")
                .lowStockAlertThreshold(10)
                .expiryAlertThreshold(5)
                .reorderThreshold(15)
                .reorderQuantity(50)
                .build();

        //***** <-- when: Create supplier --> *****//
        webTestClient.post()
                .uri("/admin/supplier")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SupplierCreationResponse.class)
                .value(response -> {
                    assertEquals("New Supplier", response.getUserName());
                    assertNotNull(response.getGeneratedPassword());
                    assertNotNull(response.getToken());
                    assertEquals("New Supplier", response.getSupplier().getName());
                });
    }

    @Test
    void shouldUpdateSupplierSuccessfully() {
        //***** <-- given: Create a supplier --> *****//
        Supplier supplier = TestUtil.createSupplier(
                userRepository,
                10,
                5,
                15,
                50,
                "Supplier A",
                "passwordA",
                passwordEncoder
        ).supplier();

        CreateSupplierRequest updateRequest = CreateSupplierRequest.builder()
                .name("Updated Supplier")
                .lowStockAlertThreshold(20)
                .expiryAlertThreshold(10)
                .reorderThreshold(30)
                .reorderQuantity(100)
                .build();

        //***** <-- when: Update supplier --> *****//
        webTestClient.put()
                .uri("/admin/supplier/" + supplier.getId())
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Supplier.class)
                .value(updatedSupplier -> {
                    assertEquals(supplier.getId(), updatedSupplier.getId());
                    assertEquals("Updated Supplier", updatedSupplier.getName());
                    assertEquals(20, updatedSupplier.getInventory().getLowStockAlertThreshold());
                    assertEquals(10, updatedSupplier.getInventory().getExpiryAlertThreshold());
                    assertEquals(30, updatedSupplier.getInventory().getReorderThreshold());
                    assertEquals(100, updatedSupplier.getInventory().getReorderQuantity());
                });
    }

    @Test
    void shouldDeleteSupplierSuccessfully() {
        //***** <-- given: Create a supplier --> *****//
        Supplier supplier = TestUtil.createSupplier(
                userRepository,
                10,
                5,
                15,
                50,
                "Supplier A",
                "passwordA",
                passwordEncoder
        ).supplier();

        //***** <-- when: Delete supplier --> *****//
        webTestClient.delete()
                .uri("/admin/supplier/" + supplier.getId())
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isNoContent();

        //***** <-- then: Verify supplier is deleted --> *****//
        assertFalse(supplierRepository.findById(supplier.getId()).isPresent());
    }
}
