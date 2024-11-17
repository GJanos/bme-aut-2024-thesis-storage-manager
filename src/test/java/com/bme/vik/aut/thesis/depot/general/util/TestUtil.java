package com.bme.vik.aut.thesis.depot.general.util;

import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.category.dto.CreateCategoryRequest;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.dto.CreateProductSchemaRequest;
import com.bme.vik.aut.thesis.depot.general.order.Order;
import com.bme.vik.aut.thesis.depot.general.order.OrderStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ExpiryStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.ProductStockResponse;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.CreateSupplierRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.SupplierCreationResponse;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.security.auth.AuthService;
import com.bme.vik.aut.thesis.depot.security.auth.dto.AuthRequest;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtil {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";


    private static final String CREATE_CATEGORY_PATH = "/admin/category";
    private static final String CREATE_PRODUCT_SCHEMA_PATH = "/admin/product-schema";
    private static final String CREATE_SUPPLIER_PATH = "/admin/supplier";
    private static final String ADD_STOCK_PATH = "/supplier/add-stock";
    private static final String CREATE_ORDER_PATH = "/order";
    private static final String ACCEPT_PENDING_ORDER_START = "/order/pending/";
    private static final String ACCEPT_PENDING_ORDER_END_PATH = "/approve";

    public static void assertCreatedAndUpdatedTimes(LocalDateTime createdAt, LocalDateTime updatedAt) {

        assertNotNull(createdAt, "createdAt should not be null");
        assertNotNull(updatedAt, "updatedAt should not be null");

        LocalDateTime nowTruncated = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime createdTruncated = createdAt.truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime updatedTruncated = updatedAt.truncatedTo(ChronoUnit.MILLIS);

        assertTrue(ChronoUnit.SECONDS.between(nowTruncated, createdTruncated) < 1, "createdAt should be recent");
        assertTrue(ChronoUnit.SECONDS.between(nowTruncated, updatedTruncated) < 1, "updatedAt should be recent");
    }

    public record CreateProductResponse(
            List<Category> categories,
            ProductSchema productSchema,
            Product product
    ) {
    }

    public record CategoryUnit(String name, String description) {
    }

    public record CreateSupplierResponse(
            Inventory inventory,
            Supplier supplier,
            MyUser user
    ) {
    }

    public static String createAndRegisterUser(
            UserRepository userRepository,
            String username,
            String password,
            Role role,
            AuthService authService,
            PasswordEncoder passwordEncoder) {

        // clear default CommandLineRunner admin user from database
        userRepository.deleteAll();

        // create and save the admin user for tests
        MyUser user = MyUser.builder()
                .userName(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();

        userRepository.save(user);

        /* Admin token is needed for accessing admin permission routes,
        so we need to authenticate the admin user and store the token.
         */
        AuthRequest authRequest = AuthRequest.builder()
                .userName(username)
                .password(password)
                .build();

        return authService.authenticate(authRequest).getToken();
    }

    public static CreateProductResponse createProduct(
            CategoryRepository categoryRepository,
            ProductSchemaRepository productSchemaRepository,
            ProductRepository productRepository,
            List<CategoryUnit> categoryUnits,
            String schemaName,
            int storageSpaceNeeded,
            String productDescription,
            long supplierId,
            ProductStatus productStatus,
            LocalDateTime expiresAt) {

        List<Category> savedCategories = categoryUnits.stream()
                .map(categoryUnit -> {
                    Category category = Category.builder()
                            .name(categoryUnit.name())
                            .description(categoryUnit.description())
                            .build();
                    return categoryRepository.save(category);
                }).toList();

        ProductSchema productSchema = ProductSchema.builder()
                .name(schemaName)
                .storageSpaceNeeded(storageSpaceNeeded)
                .categories(savedCategories)
                .build();
        ProductSchema savedSchema = productSchemaRepository.save(productSchema);

        Product product = Product.builder()
                .schema(savedSchema)
                .supplierId(supplierId)
                .description(productDescription)
                .status(productStatus)
                .expiryStatus(ExpiryStatus.NOTEXPIRED)
                .expiresAt(expiresAt)
                .build();
        Product savedProduct = productRepository.save(product);

        return new CreateProductResponse(savedCategories, savedSchema, savedProduct);
    }

    public static CreateSupplierResponse createSupplier(
            UserRepository userRepository,
            int lowStockAlertThreshold,
            int expiryAlertThreshold,
            int reorderThreshold,
            int reorderQuantity,
            String supplierName,
            String supplierPassword,
            PasswordEncoder passwordEncoder) {
        Inventory inventory = Inventory.builder()
                .usedSpace(0)
                .maxAvailableSpace(1000)
                .lowStockAlertThreshold(lowStockAlertThreshold)
                .expiryAlertThreshold(expiryAlertThreshold)
                .reorderThreshold(reorderThreshold)
                .reorderQuantity(reorderQuantity)
                .build();

        Supplier supplier = Supplier.builder()
                .name(supplierName)
                .inventory(inventory)
                .build();

        MyUser user = MyUser.builder()
                .userName(supplierName)
                .password(passwordEncoder.encode(supplierPassword))
                .role(Role.SUPPLIER)
                .supplier(supplier)
                .build();

        inventory.setSupplier(supplier);
        supplier.setUser(user);

        MyUser savedUser = userRepository.save(user);
        return new CreateSupplierResponse(
                savedUser.getSupplier().getInventory(),
                savedUser.getSupplier(),
                savedUser);
    }

    public static Category createCategoryWithAPI(
            WebTestClient webTestClient,
            String adminToken,
            String categoryName,
            String categoryDescription
            ) {
        CreateCategoryRequest createCategoryRequest = CreateCategoryRequest.builder()
                .name(categoryName)
                .description(categoryDescription)
                .build();

        return webTestClient
                .post()
                .uri(CREATE_CATEGORY_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .bodyValue(createCategoryRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Category.class)
                .value(category -> {
                    assertEquals(categoryName, category.getName());
                    assertEquals(categoryDescription, category.getDescription());
                    TestUtil.assertCreatedAndUpdatedTimes(category.getCreatedAt(), category.getUpdatedAt());
                })
                .returnResult()
                .getResponseBody();
    }

    public static ProductSchema createProductSchemaWithAPI(WebTestClient webTestClient, String adminToken, String schemaName, int storageSpaceNeeded, List<Category> categories) {
        CreateProductSchemaRequest createProductSchemaRequest = CreateProductSchemaRequest.builder()
                .name(schemaName)
                .storageSpaceNeeded(storageSpaceNeeded)
                .categoryIDs(categories.stream().map(Category::getId).toList())
                .build();

        return webTestClient
                .post()
                .uri(CREATE_PRODUCT_SCHEMA_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .bodyValue(createProductSchemaRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductSchema.class)
                .value(productSchema -> {
                    assertEquals(schemaName, productSchema.getName());
                    assertEquals(storageSpaceNeeded, productSchema.getStorageSpaceNeeded());
                    assertEquals(categories.size(), productSchema.getCategories().size());
                    TestUtil.assertCreatedAndUpdatedTimes(productSchema.getCreatedAt(), productSchema.getUpdatedAt());
                })
                .returnResult()
                .getResponseBody();
    }

    public static SupplierCreationResponse createSupplierWithAPI(
            WebTestClient webTestClient,
            String adminToken,
            String supplierName,
            int lowStockAlertThreshold,
            int expiryAlertThreshold,
            int reorderThreshold,
            int reorderQuantity
    ) {
        CreateSupplierRequest createSupplierRequest = CreateSupplierRequest.builder()
                .name(supplierName)
                .lowStockAlertThreshold(lowStockAlertThreshold)
                .expiryAlertThreshold(expiryAlertThreshold)
                .reorderThreshold(reorderThreshold)
                .reorderQuantity(reorderQuantity)
                .build();

        return webTestClient
                .post()
                .uri(CREATE_SUPPLIER_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .bodyValue(createSupplierRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SupplierCreationResponse.class)
                .value(response -> {
                    assertEquals(createSupplierRequest.getName(), response.getUserName());
                    assertNotNull(response.getGeneratedPassword());
                    assertNotNull(response.getToken());
                    assertEquals(createSupplierRequest.getName(), response.getSupplier().getName());
                    assertEquals(createSupplierRequest.getLowStockAlertThreshold(), response.getSupplier().getInventory().getLowStockAlertThreshold());
                    assertEquals(createSupplierRequest.getExpiryAlertThreshold(), response.getSupplier().getInventory().getExpiryAlertThreshold());
                    assertEquals(createSupplierRequest.getReorderThreshold(), response.getSupplier().getInventory().getReorderThreshold());
                    assertEquals(createSupplierRequest.getReorderQuantity(), response.getSupplier().getInventory().getReorderQuantity());
                })
                .returnResult()
                .getResponseBody();
    }

    public static void addStockToSupplierInventoryWithAPI(
            WebTestClient webTestClient,
            String supplierToken,
            ProductSchema productSchema,
            String productDescription,
            int stockQuantity,
            LocalDateTime expiresAt
    ) {
        CreateProductStockRequest createProductStockRequest = CreateProductStockRequest.builder()
                .productSchemaId(productSchema.getId())
                .description(productDescription)
                .quantity(stockQuantity)
                .expiresAt(expiresAt)
                .build();

        webTestClient
                .post()
                .uri(ADD_STOCK_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + supplierToken)
                .bodyValue(createProductStockRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductStockResponse.class)
                .value(productStockResponse -> {
                    assertEquals(productSchema.getId(), productStockResponse.getProductSchemaId());
                    assertEquals(stockQuantity, productStockResponse.getQuantity());
                    assertEquals("Stock added successfully", productStockResponse.getResponse());
                });
    }

    public static Order createOrderWithAPI(
            WebTestClient webTestClient,
            String userToken,
            List<OrderItem> orderItems,
            int fullOrderSize
    ) {
        List<Map<String, Object>> orderItemRequests = orderItems.stream()
                .map(OrderItem::toOrderItemRequest)
                .toList();

        return webTestClient
                .post()
                .uri(CREATE_ORDER_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + userToken)
                .bodyValue(orderItemRequests)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Order.class)
                .value(order -> {
                    assertNotNull(order.getId());
                    assertEquals(OrderStatus.PENDING, order.getStatus());
                    assertEquals(fullOrderSize, order.getOrderItems().size());

                    // Assert all products in the order
                    for (int i = 0; i < orderItems.size(); i++) {
                        Product orderProduct = order.getOrderItems().get(i);
                        OrderItem orderItem = orderItems.get(i);

                        // Assert product properties
                        assertNotNull(orderProduct.getId());
                        assertNotNull(orderProduct.getSchema());

                        if (orderItem instanceof OrderItemByProdId orderItemByProdId) {
                            assertEquals(orderItemByProdId.getProductId(), orderProduct.getSchema().getId());
                        } else if (orderItem instanceof OrderItemByProdName orderItemByProdName) {
                            assertEquals(orderItemByProdName.getProductName(), orderProduct.getSchema().getName());
                        } else if (orderItem instanceof OrderItemByProdSupplName orderItemByProdSupplName) {
                            assertEquals(orderItemByProdSupplName.getProductName(), orderProduct.getSchema().getName());
                            // TODO assertEquals(orderItemByProdSupplName.getSupplierName(), orderProduct.().getName());
                        }

                        // Assert timestamps
                        assertNotNull(orderProduct.getCreatedAt());
                        assertNotNull(orderProduct.getUpdatedAt());
                    }
                })
                .returnResult()
                .getResponseBody();
    }

    public static void acceptPendingOrderWithAPI(
            WebTestClient webTestClient,
            String adminToken,
            Order order
    ) {
        String fullAcceptPath = ACCEPT_PENDING_ORDER_START + order.getId() + ACCEPT_PENDING_ORDER_END_PATH;
        webTestClient
                .put()
                .uri(fullAcceptPath)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Order.class)
                .value(acceptedOrder -> {
                    assertEquals(order.getId(), acceptedOrder.getId());
                    assertEquals(OrderStatus.COMPLETED, acceptedOrder.getStatus());
                    assertEquals(order.getOrderItems().size(), acceptedOrder.getOrderItems().size());

                    // Assert all products in the completed order
                    for (int i = 0; i < order.getOrderItems().size(); i++) {
                        Product originalProduct = order.getOrderItems().get(i);
                        Product acceptedProduct = acceptedOrder.getOrderItems().get(i);

                        // Assert product properties
                        assertEquals(originalProduct.getId(), acceptedProduct.getId());
                        assertEquals(originalProduct.getSchema().getId(), acceptedProduct.getSchema().getId());
                        assertEquals(originalProduct.getSupplierId(), acceptedProduct.getSupplierId());
                        assertEquals(originalProduct.getDescription(), acceptedProduct.getDescription());
                        assertEquals(ProductStatus.REMOVED, acceptedProduct.getStatus());
                        assertEquals(originalProduct.getExpiryStatus(), acceptedProduct.getExpiryStatus());

                        // Assert timestamps
                        assertEquals(originalProduct.getCreatedAt(), acceptedProduct.getCreatedAt());
                        assertTrue(acceptedProduct.getUpdatedAt().isAfter(originalProduct.getUpdatedAt()));
                    }
                });
    }


}
