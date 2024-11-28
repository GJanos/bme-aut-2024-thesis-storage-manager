package com.bme.vik.aut.thesis.depot.general.alert;

import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.order.Order;
import com.bme.vik.aut.thesis.depot.general.order.OrderRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.*;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierService;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.SupplierCreationResponse;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.util.OrderItem;
import com.bme.vik.aut.thesis.depot.general.util.OrderItemByProdName;
import com.bme.vik.aut.thesis.depot.general.util.OrderItemByProdSupplName;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class AlertIntegrationTest {

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
    private InventoryService inventoryService;
    @Autowired
    private AlertService alertService;
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
    @Autowired
    private ProductService productService;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(alertService, "AUTO_REORDER_ENABLED", true);
        ReflectionTestUtils.setField(alertService, "LOW_STOCK_ALERT_ENABLED", true);
        ReflectionTestUtils.setField(alertService, "EXPIRY_ALERT_ENABLED", true);

        alertService.setInventoryService(inventoryService);

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
    void shouldEmitLowStockAndReorderEventsAndReplenishStock() {
        //***** <-- given: Create a product schema, supplier, and add stock --> *****//
        String categoryName = "Category 1";
        String categoryDescription = "Test category";

        Category category = TestUtil.createCategoryWithAPI(webTestClient, ADMIN_TOKEN, categoryName, categoryDescription);

        String schemaName = "Test Product";
        int storageSpaceNeeded = 10;

        ProductSchema productSchema = TestUtil.createProductSchemaWithAPI(webTestClient, ADMIN_TOKEN, schemaName, storageSpaceNeeded, new ArrayList<>(List.of(category)));

        int lowStockAlertThreshold = 4;
        int expiryAlertThreshold = 5;
        int reorderThreshold = 2;
        int reorderQuantity = 1;
        String supplierName = "Test Supplier";

        SupplierCreationResponse response = TestUtil.createSupplierWithAPI(webTestClient, ADMIN_TOKEN, supplierName, lowStockAlertThreshold, expiryAlertThreshold, reorderThreshold, reorderQuantity);
        SUPPLIER_TOKEN = response.getToken();

                String productDescription = "Test Product Description";
        int stockQuantity = 5;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(10);

        TestUtil.addStockToSupplierInventoryWithAPI(webTestClient, SUPPLIER_TOKEN, productSchema, productDescription, stockQuantity, expiresAt);

        List<OrderItem> orderItems = List.of(
                new OrderItemByProdName(productSchema.getName(), 2),
                new OrderItemByProdSupplName(productSchema.getName(), supplierName, 2)
        );
        int fullOrderSize = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();

        Order order = TestUtil.createOrderWithAPI(webTestClient, SUPPLIER_TOKEN, orderItems, fullOrderSize);

        TestUtil.acceptPendingOrderWithAPI(webTestClient, ADMIN_TOKEN, order);

        //***** <-- then: Verify stock was because of AUTO_REORDER_ENABLED --> *****//
        Inventory inventory = inventoryService.getInventoryById(response.getSupplier().getInventory().getId());

        // size became larger, because products are not removed from inventory, only their state changes to REMOVED
        assertEquals(stockQuantity + reorderQuantity, inventory.getProductIds().size());

        // Verify one new product was added (reorderQuantity = 1)
        Product newProduct = productService.getProductById(inventory.getProductIds().get(inventory.getProductIds().size() - 1));

        assertEquals(productSchema.getId(), newProduct.getSchema().getId());
        assertEquals(supplierService.getSupplierByName(supplierName).getId(), newProduct.getSupplierId());
        assertEquals(productDescription, newProduct.getDescription());
        assertEquals(ProductStatus.FREE, newProduct.getStatus());
        assertEquals(ExpiryStatus.NOTEXPIRED, newProduct.getExpiryStatus());
    }

    @Test
    void shouldEmitExpiredProductEventAndChangeProductStatus() {
        //***** <-- given: Create a category --> *****//
        String categoryName = "Category 1";
        String categoryDescription = "Test category";

        Category category = TestUtil.createCategoryWithAPI(webTestClient, ADMIN_TOKEN, categoryName, categoryDescription);

        //***** <-- given: Create a product schema --> *****//
        String schemaName = "Test Product";
        int storageSpaceNeeded = 10;

        ProductSchema productSchema = TestUtil.createProductSchemaWithAPI(webTestClient, ADMIN_TOKEN, schemaName, storageSpaceNeeded, List.of(category));

        //***** <-- given: Create a supplier --> *****//
        int lowStockAlertThreshold = 4;
        int expiryAlertThreshold = 5;
        int reorderThreshold = 2;
        int reorderQuantity = 1;
        String supplierName = "Test Supplier";

        SupplierCreationResponse supplierResponse = TestUtil.createSupplierWithAPI(webTestClient, ADMIN_TOKEN, supplierName, lowStockAlertThreshold, expiryAlertThreshold, reorderThreshold, reorderQuantity);
        SUPPLIER_TOKEN = supplierResponse.getToken();

        //***** <-- given: Add stock with varying expiry dates --> *****//
        LocalDateTime now = LocalDateTime.now();
        List<CreateProductStockRequest> stocks = List.of(
                new CreateProductStockRequest(productSchema.getId(), "Long Expired Product", 1, now.minusDays(31)), // LONGEXPIRED
                new CreateProductStockRequest(productSchema.getId(), "Expired Product", 1, now.minusDays(1)),     // EXPIRED
                new CreateProductStockRequest(productSchema.getId(), "Soon to Expire Product", 1, now.plusDays(4)), // SOONTOEXPIRE
                new CreateProductStockRequest(productSchema.getId(), "Not Expired Product", 1, now.plusDays(10))   // NOTEXPIRED
        );

        for (CreateProductStockRequest stock : stocks) {
            TestUtil.addStockToSupplierInventoryWithAPI(webTestClient, SUPPLIER_TOKEN, productSchema, stock.getDescription(), stock.getQuantity(), stock.getExpiresAt());
        }

        //***** <-- when: Trigger the scheduled expiry check --> *****//
        alertService.checkForExpiredProducts();

        //***** <-- then: Verify updated expiry statuses in the inventory --> *****//
        Inventory inventory = inventoryService.getInventoryById(supplierResponse.getSupplier().getInventory().getId());
        List<Product> inventoryProducts = inventory.getProductIds().stream()
                .map(productService::getProductById)
                .toList();

        assertEquals(4, inventoryProducts.size());

        for (Product product : inventoryProducts) {
            switch (product.getDescription()) {
                case "Long Expired Product" -> assertEquals(ExpiryStatus.LONGEXPIRED, product.getExpiryStatus());
                case "Expired Product" -> assertEquals(ExpiryStatus.EXPIRED, product.getExpiryStatus());
                case "Soon to Expire Product" -> assertEquals(ExpiryStatus.SOONTOEXPIRE, product.getExpiryStatus());
                case "Not Expired Product" -> assertEquals(ExpiryStatus.NOTEXPIRED, product.getExpiryStatus());
            }
        }
    }
}
