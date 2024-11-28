package com.bme.vik.aut.thesis.depot.general.report;

import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.order.Order;
import com.bme.vik.aut.thesis.depot.general.order.OrderRepository;
import com.bme.vik.aut.thesis.depot.general.order.OrderStatus;
import com.bme.vik.aut.thesis.depot.general.report.dto.*;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ExpiryStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.SupplierCreationResponse;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.util.OrderItem;
import com.bme.vik.aut.thesis.depot.general.util.OrderItemByProdName;
import com.bme.vik.aut.thesis.depot.general.util.TestUtil;
import com.bme.vik.aut.thesis.depot.security.auth.AuthService;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class ReportControllerIntegrationTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INVENTORY_STATE_REPORT_PATH = "/report/inventory/state";
    private static final String INVENTORY_EXPIRY_REPORT_PATH = "/report/inventory/expiry";
    private static final String ORDER_REPORT_PATH = "/report/order";
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductSchemaRepository productSchemaRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private AuthService authService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String ADMIN_TOKEN;
    private String SUPPLIER_TOKEN;
    @Value("${custom.admin.username}")
    private String adminUsername;
    @Value("${custom.admin.password}")
    private String adminPassword;

    @Value("${custom.inventory.max-depot-space}")
    private int MAX_AVAILABLE_DEPOT_SPACE;

    @BeforeEach
    void setUp() {
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
        orderRepository.deleteAll();
        productRepository.deleteAll();
        productSchemaRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        inventoryService.clearStock();
    }

    @Test
    void shouldGetInventoryStateReportWithMultipleSuppliers() {
        //***** <-- given: Set up category --> *****//
        String categoryName = "Category 1";
        String categoryDescription = "Test category";
        Category category = TestUtil.createCategoryWithAPI(webTestClient, ADMIN_TOKEN, categoryName, categoryDescription);

        String schemaName = "Test Product";
        int storageSpaceNeeded = 10;

        //***** <-- given: Create product schema --> *****//
        ProductSchema productSchema = TestUtil.createProductSchemaWithAPI(webTestClient, ADMIN_TOKEN, schemaName, storageSpaceNeeded, List.of(category));

        //***** <-- given: Create supplier 1 and add stock --> *****//
        String supplierName1 = "Supplier 1";
        int lowStockAlertThreshold1 = 5;
        int expiryAlertThreshold1 = 7;
        int reorderThreshold1 = 3;
        int reorderQuantity1 = 5;

        SupplierCreationResponse supplierResponse1 = TestUtil.createSupplierWithAPI(
                webTestClient, ADMIN_TOKEN, supplierName1, lowStockAlertThreshold1, expiryAlertThreshold1, reorderThreshold1, reorderQuantity1);
        String supplierToken1 = supplierResponse1.getToken();

        String productDescription1 = "Product for Supplier 1";
        int stockQuantity1 = 15;
        LocalDateTime expiresAt1 = LocalDateTime.now().plusDays(10);

        TestUtil.addStockToSupplierInventoryWithAPI(webTestClient, supplierToken1, productSchema, productDescription1, stockQuantity1, expiresAt1);

        //***** <-- given: Create supplier 2 and add stock --> *****//
        String supplierName2 = "Supplier 2";
        int lowStockAlertThreshold2 = 4;
        int expiryAlertThreshold2 = 6;
        int reorderThreshold2 = 2;
        int reorderQuantity2 = 4;

        SupplierCreationResponse supplierResponse2 = TestUtil.createSupplierWithAPI(
                webTestClient, ADMIN_TOKEN, supplierName2, lowStockAlertThreshold2, expiryAlertThreshold2, reorderThreshold2, reorderQuantity2);
        String supplierToken2 = supplierResponse2.getToken();

        String productDescription2 = "Product for Supplier 2";
        int stockQuantity2 = 10;
        LocalDateTime expiresAt2 = LocalDateTime.now().plusDays(20);

        TestUtil.addStockToSupplierInventoryWithAPI(webTestClient, supplierToken2, productSchema, productDescription2, stockQuantity2, expiresAt2);

        //***** <-- when: Call inventory state report endpoint --> *****//
        InventoryStateReportResponse reportResponse = webTestClient
                .get()
                .uri(INVENTORY_STATE_REPORT_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(InventoryStateReportResponse.class)
                .returnResult()
                .getResponseBody();

        //***** <-- then: Verify the report details --> *****//
        assertNotNull(reportResponse);
        assertEquals(MAX_AVAILABLE_DEPOT_SPACE, reportResponse.getMaxAvailableSpaceInStorage());
        assertEquals(2, reportResponse.getNumOfInventories());
        assertEquals((stockQuantity1 + stockQuantity2) * storageSpaceNeeded, reportResponse.getUsedSpaceInStorage());
        assertEquals(2, reportResponse.getInventoryStates().size());

        // Verify the first inventory state
        InventoryState inventoryState1 = reportResponse.getInventoryStates().get(0);
        assertEquals(supplierResponse1.getSupplier().getInventory().getId(), inventoryState1.getInventoryID());
        assertEquals(supplierResponse1.getSupplier().getId(), inventoryState1.getSupplierID());
        assertEquals(supplierName1, inventoryState1.getSupplierName());
        assertEquals(stockQuantity1 * storageSpaceNeeded, inventoryState1.getUsedSpace());
        assertEquals(1, inventoryState1.getStock().size());

        // Verify the first product state in the first inventory
        List<ProductState> productStates1 = inventoryState1.getStock().get(productSchema.getId());
        assertNotNull(productStates1);
        assertEquals(stockQuantity1, productStates1.size());

        ProductState firstProductState1 = productStates1.get(0);
        assertEquals(productSchema.getId(), firstProductState1.getProductSchemaID());
        assertEquals(schemaName, firstProductState1.getProductName());
        assertEquals(storageSpaceNeeded, firstProductState1.getStorageSpaceNeeded());

        // Verify the second inventory state
        InventoryState inventoryState2 = reportResponse.getInventoryStates().get(1);
        assertEquals(supplierResponse2.getSupplier().getInventory().getId(), inventoryState2.getInventoryID());
        assertEquals(supplierResponse2.getSupplier().getId(), inventoryState2.getSupplierID());
        assertEquals(supplierName2, inventoryState2.getSupplierName());
        assertEquals(stockQuantity2 * storageSpaceNeeded, inventoryState2.getUsedSpace());
        assertEquals(1, inventoryState2.getStock().size());

        // Verify the first product state in the second inventory
        List<ProductState> productStates2 = inventoryState2.getStock().get(productSchema.getId());
        assertNotNull(productStates2);
        assertEquals(stockQuantity2, productStates2.size());

        ProductState firstProductState2 = productStates2.get(0);
        assertEquals(productSchema.getId(), firstProductState2.getProductSchemaID());
        assertEquals(schemaName, firstProductState2.getProductName());
        assertEquals(storageSpaceNeeded, firstProductState2.getStorageSpaceNeeded());
    }

    @Test
    void shouldGetInventoryExpiryReport() {
        //***** <-- given: Set up category, product schema, supplier, and inventory with varying expiry dates --> *****//
        String categoryName = "Category 1";
        String categoryDescription = "Test category";
        Category category = TestUtil.createCategoryWithAPI(webTestClient, ADMIN_TOKEN, categoryName, categoryDescription);

        String schemaName = "Test Product";
        int storageSpaceNeeded = 10;
        ProductSchema productSchema = TestUtil.createProductSchemaWithAPI(webTestClient, ADMIN_TOKEN, schemaName, storageSpaceNeeded, List.of(category));

        String supplierName = "Test Supplier";
        int lowStockAlertThreshold = 5;
        int expiryAlertThreshold = 7;
        int reorderThreshold = 3;
        int reorderQuantity = 5;
        SupplierCreationResponse supplierResponse = TestUtil.createSupplierWithAPI(
                webTestClient, ADMIN_TOKEN, supplierName, lowStockAlertThreshold, expiryAlertThreshold, reorderThreshold, reorderQuantity);
        SUPPLIER_TOKEN = supplierResponse.getToken();

        List<CreateProductStockRequest> stocks = List.of(
                new CreateProductStockRequest(productSchema.getId(), "Long Expired Product", 1, LocalDateTime.now().minusDays(31)),
                new CreateProductStockRequest(productSchema.getId(), "Expired Product", 1, LocalDateTime.now().minusDays(1)),
                new CreateProductStockRequest(productSchema.getId(), "Soon to Expire Product", 1, LocalDateTime.now().plusDays(3)),
                new CreateProductStockRequest(productSchema.getId(), "Not Expired Product", 1, LocalDateTime.now().plusDays(10))
        );

        for (CreateProductStockRequest stock : stocks) {
            TestUtil.addStockToSupplierInventoryWithAPI(webTestClient, SUPPLIER_TOKEN, productSchema, stock.getDescription(), stock.getQuantity(), stock.getExpiresAt());
        }

        //***** <-- when: Call inventory expiry report endpoint --> *****//
        InventoryExpiryReportResponse reportResponse = webTestClient
                .get()
                .uri(INVENTORY_EXPIRY_REPORT_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(InventoryExpiryReportResponse.class)
                .returnResult()
                .getResponseBody();

        //***** <-- then: Verify the report details --> *****//
        assertNotNull(reportResponse);
        assertEquals(4, reportResponse.getDepotExpiryStats().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(1, reportResponse.getDepotExpiryStats().get(ExpiryStatus.LONGEXPIRED));
        assertEquals(1, reportResponse.getDepotExpiryStats().get(ExpiryStatus.EXPIRED));
        assertEquals(1, reportResponse.getDepotExpiryStats().get(ExpiryStatus.SOONTOEXPIRE));
        assertEquals(1, reportResponse.getDepotExpiryStats().get(ExpiryStatus.NOTEXPIRED));
        assertEquals(1, reportResponse.getInventoryExpires().size());

        InventoryExpiry inventoryExpiry = reportResponse.getInventoryExpires().get(0);

        // Verify `InventoryExpiry` details
        assertEquals(supplierResponse.getSupplier().getInventory().getId(), inventoryExpiry.getInventoryID());
        assertEquals(supplierResponse.getSupplier().getId(), inventoryExpiry.getSupplierID());
        assertEquals(supplierName, inventoryExpiry.getSupplierName());
        assertNotNull(inventoryExpiry.getCreatedAt());
        assertNotNull(inventoryExpiry.getUpdatedAt());

        // Verify `stock` map size and content
        Map<ExpiryStatus, List<ProductExpiry>> stock = inventoryExpiry.getStock();
        assertEquals(4, stock.size());
        assertTrue(stock.containsKey(ExpiryStatus.LONGEXPIRED));
        assertTrue(stock.containsKey(ExpiryStatus.EXPIRED));
        assertTrue(stock.containsKey(ExpiryStatus.SOONTOEXPIRE));
        assertTrue(stock.containsKey(ExpiryStatus.NOTEXPIRED));

        // Verify each `ProductExpiry` within the `stock`
        ProductExpiry longExpiredProduct = stock.get(ExpiryStatus.LONGEXPIRED).get(0);
        assertEquals("Test Product", longExpiredProduct.getProductName());
        assertEquals(ExpiryStatus.LONGEXPIRED, longExpiredProduct.getExpiryStatus());
        assertNotNull(longExpiredProduct.getCreatedAt());
        assertNotNull(longExpiredProduct.getUpdatedAt());

        ProductExpiry expiredProduct = stock.get(ExpiryStatus.EXPIRED).get(0);
        assertEquals("Test Product", expiredProduct.getProductName());
        assertEquals(ExpiryStatus.EXPIRED, expiredProduct.getExpiryStatus());
        assertNotNull(expiredProduct.getCreatedAt());
        assertNotNull(expiredProduct.getUpdatedAt());

        ProductExpiry soonToExpireProduct = stock.get(ExpiryStatus.SOONTOEXPIRE).get(0);
        assertEquals("Test Product", soonToExpireProduct.getProductName());
        assertEquals(ExpiryStatus.SOONTOEXPIRE, soonToExpireProduct.getExpiryStatus());
        assertNotNull(soonToExpireProduct.getCreatedAt());
        assertNotNull(soonToExpireProduct.getUpdatedAt());

        ProductExpiry notExpiredProduct = stock.get(ExpiryStatus.NOTEXPIRED).get(0);
        assertEquals("Test Product", notExpiredProduct.getProductName());
        assertEquals(ExpiryStatus.NOTEXPIRED, notExpiredProduct.getExpiryStatus());
        assertNotNull(notExpiredProduct.getCreatedAt());
        assertNotNull(notExpiredProduct.getUpdatedAt());
    }

    @Test
    void shouldGetOrderReport() {
        //***** <-- given: Set up orders for multiple users --> *****//
        String schemaName = "Test Product";
        int storageSpaceNeeded = 10;

        String categoryName = "Category 1";
        String categoryDescription = "Test category";
        Category category = TestUtil.createCategoryWithAPI(webTestClient, ADMIN_TOKEN, categoryName, categoryDescription);

        ProductSchema productSchema = TestUtil.createProductSchemaWithAPI(webTestClient, ADMIN_TOKEN, schemaName, storageSpaceNeeded, List.of(category));

        String supplierName = "Test Supplier";
        SupplierCreationResponse supplierResponse = TestUtil.createSupplierWithAPI(
                webTestClient, ADMIN_TOKEN, supplierName, 5, 7, 3, 5);
        SUPPLIER_TOKEN = supplierResponse.getToken();

        TestUtil.addStockToSupplierInventoryWithAPI(webTestClient, SUPPLIER_TOKEN, productSchema, "Test Product Description", 10, LocalDateTime.now().plusDays(10));

        List<OrderItem> orderItems = List.of(
                new OrderItemByProdName(productSchema.getName(), 3)
        );
        Order order = TestUtil.createOrderWithAPI(webTestClient, SUPPLIER_TOKEN, orderItems, 3);

        TestUtil.acceptPendingOrderWithAPI(webTestClient, ADMIN_TOKEN, order);

        //***** <-- when: Call order report endpoint --> *****//
        OrderReportResponse reportResponse = webTestClient
                .get()
                .uri(ORDER_REPORT_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderReportResponse.class)
                .returnResult()
                .getResponseBody();

        //***** <-- then: Verify the report details --> *****//
        assertNotNull(reportResponse);
        assertEquals(1, reportResponse.getNumOfOrders());
        assertEquals(1, reportResponse.getUserOrders().size());
        assertEquals(1, reportResponse.getOrderStats().size());
        assertEquals(1, reportResponse.getOrderStats().get(OrderStatus.COMPLETED));

        UserOrder userOrder = reportResponse.getUserOrders().get(0);

        // Verify `UserOrder` details
        assertNotNull(userOrder);
        assertEquals(order.getId(), userOrder.getOrderID());
        assertEquals(order.getUserId(), userOrder.getUserID());
        assertEquals(OrderStatus.COMPLETED, userOrder.getOrderStatus());
        assertNotNull(userOrder.getCreatedAt());
        assertEquals(3, userOrder.getProducts().size());

        ProductState productState = userOrder.getProducts().get(0);

        // Verify `ProductState` details
        assertNotNull(productState);
        assertEquals(productSchema.getId(), productState.getProductSchemaID());
        assertEquals(productSchema.getName(), productState.getProductName());
        assertEquals(ProductStatus.REMOVED, productState.getStatus());
        assertEquals(storageSpaceNeeded, productState.getStorageSpaceNeeded());
        assertEquals(1, productState.getCategoryIDs().size());
        assertEquals(category.getId(), productState.getCategoryIDs().get(0));
        assertNotNull(productState.getCreatedAt());
        assertNotNull(productState.getUpdatedAt());
    }
}
