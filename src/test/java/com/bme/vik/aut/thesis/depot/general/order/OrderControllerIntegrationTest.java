package com.bme.vik.aut.thesis.depot.general.order;


import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.*;
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
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class OrderControllerIntegrationTest {


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
    void shouldReturnAllOrders() {
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

        //***** <-- Create first order --> *****//
        List<OrderItem> orderItems1 = List.of(
                new OrderItemByProdName(productSchema.getName(), 1),
                new OrderItemByProdSupplName(productSchema.getName(), supplierName, 1)
        );
        int fullOrderSize1 = orderItems1.stream().mapToInt(OrderItem::getQuantity).sum();

        Order order1 = TestUtil.createOrderWithAPI(webTestClient, SUPPLIER_TOKEN, orderItems1, fullOrderSize1);

        //***** <-- Create second order with a different product description --> *****//
        String secondProductDescription = "Test Product Description 2";
        TestUtil.addStockToSupplierInventoryWithAPI(webTestClient, SUPPLIER_TOKEN, productSchema, secondProductDescription, stockQuantity, expiresAt);

        List<OrderItem> orderItems2 = List.of(
                new OrderItemByProdName(productSchema.getName(), 2),
                new OrderItemByProdSupplName(productSchema.getName(), supplierName, 2)
        );
        int fullOrderSize2 = orderItems2.stream().mapToInt(OrderItem::getQuantity).sum();

        Order order2 = TestUtil.createOrderWithAPI(webTestClient, SUPPLIER_TOKEN, orderItems2, fullOrderSize2);

        //***** <-- when: Fetch all orders via the API --> *****//
        List<Order> orders = webTestClient.get()
                .uri("/order")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Order.class)
                .returnResult()
                .getResponseBody();

        //***** <-- then: Verify the returned orders --> *****//
        assertNotNull(orders);
        assertEquals(2, orders.size());

        Order returnedOrder1 = orders.get(0);
        Order returnedOrder2 = orders.get(1);

        // Assertions for order1
        assertEquals(order1.getId(), returnedOrder1.getId());
        assertEquals(fullOrderSize1, returnedOrder1.getOrderItems().size());
        assertEquals(OrderStatus.PENDING, returnedOrder1.getStatus());

        // Assertions for order2
        assertEquals(order2.getId(), returnedOrder2.getId());
        assertEquals(fullOrderSize2, returnedOrder2.getOrderItems().size());
        assertEquals(OrderStatus.PENDING, returnedOrder2.getStatus());

        // Verify inventory status after orders
        Inventory inventory = inventoryService.getInventoryById(response.getSupplier().getInventory().getId());
        assertEquals(stockQuantity * 2, inventory.getProductIds().size());

        // Verify one new product was added (reorderQuantity = 1)
        Product newProduct = productService.getProductById(inventory.getProductIds().get(inventory.getProductIds().size() - 1));

        assertEquals(productSchema.getId(), newProduct.getSchema().getId());
        assertEquals(supplierService.getSupplierByName(supplierName).getId(), newProduct.getSupplierId());
        assertEquals(secondProductDescription, newProduct.getDescription());
        assertEquals(ProductStatus.FREE, newProduct.getStatus());
        assertEquals(ExpiryStatus.NOTEXPIRED, newProduct.getExpiryStatus());
    }

    @Test
    void shouldReturnOrderById() {
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

        //***** <-- Create an order --> *****//
        List<OrderItem> orderItems = List.of(
                new OrderItemByProdName(productSchema.getName(), 2),
                new OrderItemByProdSupplName(productSchema.getName(), supplierName, 2)
        );
        int fullOrderSize = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();

        Order createdOrder = TestUtil.createOrderWithAPI(webTestClient, SUPPLIER_TOKEN, orderItems, fullOrderSize);

        //***** <-- when: Fetch the order by its ID via the API --> *****//
        Order returnedOrder = webTestClient.get()
                .uri("/order/" + createdOrder.getId())
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class)
                .returnResult()
                .getResponseBody();

        //***** <-- then: Validate the returned order --> *****//
        assertNotNull(returnedOrder);
        assertEquals(createdOrder.getId(), returnedOrder.getId());
        assertEquals(OrderStatus.PENDING, returnedOrder.getStatus());
        assertEquals(fullOrderSize, returnedOrder.getOrderItems().size());

        // Validate inventory updates
        Inventory inventory = inventoryService.getInventoryById(response.getSupplier().getInventory().getId());
        // order was not accepted yet, reorder did not happen
        assertEquals(stockQuantity, inventory.getProductIds().size());

        Product newProduct = productService.getProductById(inventory.getProductIds().get(inventory.getProductIds().size() - 1));

        assertEquals(productSchema.getId(), newProduct.getSchema().getId());
        assertEquals(supplierService.getSupplierByName(supplierName).getId(), newProduct.getSupplierId());
        assertEquals(productDescription, newProduct.getDescription());
        assertEquals(ProductStatus.FREE, newProduct.getStatus());
        assertEquals(ExpiryStatus.NOTEXPIRED, newProduct.getExpiryStatus());
    }

    @Test
    void shouldCancelOrderSuccessfully() {
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

        SupplierCreationResponse supplierResponse = TestUtil.createSupplierWithAPI(webTestClient, ADMIN_TOKEN, supplierName, lowStockAlertThreshold, expiryAlertThreshold, reorderThreshold, reorderQuantity);
        SUPPLIER_TOKEN = supplierResponse.getToken();

        String productDescription = "Test Product Description";
        int stockQuantity = 5;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(10);

        TestUtil.addStockToSupplierInventoryWithAPI(webTestClient, SUPPLIER_TOKEN, productSchema, productDescription, stockQuantity, expiresAt);

        //***** <-- Create an order --> *****//
        List<OrderItem> orderItems = List.of(
                new OrderItemByProdName(productSchema.getName(), 2),
                new OrderItemByProdSupplName(productSchema.getName(), supplierName, 2)
        );
        int fullOrderSize = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();

        Order createdOrder = TestUtil.createOrderWithAPI(webTestClient, SUPPLIER_TOKEN, orderItems, fullOrderSize);

        //***** <-- when: Cancel the order via the API --> *****//
        webTestClient.delete()
                .uri("/order/" + createdOrder.getId())
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isNoContent();

        //***** <-- then: Verify the order is cancelled and products are freed --> *****//
        // Fetch the order and verify its status
        Order cancelledOrder = orderRepository.findById(createdOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());

        // Fetch the inventory and verify product statuses
        Inventory inventory = inventoryService.getInventoryById(supplierResponse.getSupplier().getInventory().getId());
        List<Product> inventoryProducts = inventory.getProductIds().stream()
                .map(productService::getProductById)
                .toList();

        assertEquals(stockQuantity, inventoryProducts.size()); // Stock is consistent
        inventoryProducts.forEach(product -> {
            assertEquals(ProductStatus.FREE, product.getStatus()); // Products are set to FREE
        });
    }

    @Test
    void shouldReturnAllPendingOrdersSuccessfully() {
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

        SupplierCreationResponse supplierResponse = TestUtil.createSupplierWithAPI(webTestClient, ADMIN_TOKEN, supplierName, lowStockAlertThreshold, expiryAlertThreshold, reorderThreshold, reorderQuantity);
        SUPPLIER_TOKEN = supplierResponse.getToken();

        String productDescription = "Test Product Description";
        int stockQuantity = 5;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(10);

        TestUtil.addStockToSupplierInventoryWithAPI(webTestClient, SUPPLIER_TOKEN, productSchema, productDescription, stockQuantity, expiresAt);

        //***** <-- Create two pending orders --> *****//
        List<OrderItem> orderItems1 = List.of(
                new OrderItemByProdName(productSchema.getName(), 1),
                new OrderItemByProdSupplName(productSchema.getName(), supplierName, 1)
        );
        int fullOrderSize1 = orderItems1.stream().mapToInt(OrderItem::getQuantity).sum();
        Order order1 = TestUtil.createOrderWithAPI(webTestClient, SUPPLIER_TOKEN, orderItems1, fullOrderSize1);

        List<OrderItem> orderItems2 = List.of(
                new OrderItemByProdName(productSchema.getName(), 2)
        );
        int fullOrderSize2 = orderItems2.stream().mapToInt(OrderItem::getQuantity).sum();
        Order order2 = TestUtil.createOrderWithAPI(webTestClient, SUPPLIER_TOKEN, orderItems2, fullOrderSize2);

        //***** <-- when: Fetch all pending orders --> *****//
        List<Order> pendingOrders = webTestClient.get()
                .uri("/order/pending")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Order.class)
                .returnResult()
                .getResponseBody();

        //***** <-- then: Verify the fetched pending orders --> *****//
        assertNotNull(pendingOrders);
        assertEquals(2, pendingOrders.size());

        Order pendingOrder1 = pendingOrders.get(0);
        Order pendingOrder2 = pendingOrders.get(1);

        assertEquals(order1.getId(), pendingOrder1.getId());
        assertEquals(OrderStatus.PENDING, pendingOrder1.getStatus());
        assertEquals(fullOrderSize1, pendingOrder1.getOrderItems().size());

        assertEquals(order2.getId(), pendingOrder2.getId());
        assertEquals(OrderStatus.PENDING, pendingOrder2.getStatus());
        assertEquals(fullOrderSize2, pendingOrder2.getOrderItems().size());
    }

    @Test
    void shouldApprovePendingOrderSuccessfully() {
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

        SupplierCreationResponse supplierResponse = TestUtil.createSupplierWithAPI(webTestClient, ADMIN_TOKEN, supplierName, lowStockAlertThreshold, expiryAlertThreshold, reorderThreshold, reorderQuantity);
        SUPPLIER_TOKEN = supplierResponse.getToken();

        String productDescription = "Test Product Description";
        int stockQuantity = 5;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(10);

        TestUtil.addStockToSupplierInventoryWithAPI(webTestClient, SUPPLIER_TOKEN, productSchema, productDescription, stockQuantity, expiresAt);

        //***** <-- Create a pending order --> *****//
        List<OrderItem> orderItems = List.of(
                new OrderItemByProdName(productSchema.getName(), 2),
                new OrderItemByProdSupplName(productSchema.getName(), supplierName, 1)
        );
        int fullOrderSize = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();

        Order pendingOrder = TestUtil.createOrderWithAPI(webTestClient, SUPPLIER_TOKEN, orderItems, fullOrderSize);

        //***** <-- when: Approve the pending order --> *****//
        Order approvedOrder = webTestClient.put()
                .uri("/order/pending/" + pendingOrder.getId() + "/approve")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Order.class)
                .returnResult()
                .getResponseBody();

        //***** <-- then: Verify the approved order --> *****//
        assertNotNull(approvedOrder);
        assertEquals(pendingOrder.getId(), approvedOrder.getId());
        assertEquals(OrderStatus.COMPLETED, approvedOrder.getStatus());

        // Verify that the products are marked as removed
        List<Product> orderProducts = pendingOrder.getOrderItems();
        orderProducts.forEach(product -> {
            Product updatedProduct = productService.getProductById(product.getId());
            assertEquals(ProductStatus.REMOVED, updatedProduct.getStatus());
        });
    }

}