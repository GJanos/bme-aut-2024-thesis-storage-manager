package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.util.TestUtil;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.info.dto.OrderItemResponse;
import com.bme.vik.aut.thesis.depot.general.info.dto.OrderResponse;
import com.bme.vik.aut.thesis.depot.general.info.dto.ProductResponse;
import com.bme.vik.aut.thesis.depot.general.info.dto.SupplierResponse;
import com.bme.vik.aut.thesis.depot.general.order.Order;
import com.bme.vik.aut.thesis.depot.general.order.OrderRepository;
import com.bme.vik.aut.thesis.depot.general.order.OrderStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.user.UserRepository;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.auth.AuthService;
import com.bme.vik.aut.thesis.depot.security.auth.dto.RegisterRequest;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class InfoControllerIntegrationTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String INFO_USER_ME_PATH = "/info/user/me";
    private static final String INFO_USER_PRODUCT_PATH = "/info/product";
    private static final String INFO_USER_PRODUCTS_OF_CATEGORY_PATH = "/info/product/category/";
    private static final String INFO_USER_SUPPLIER_PATH = "/info/supplier";
    private static final String INFO_USER_OWN_ORDER_PATH = "/info/order";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductSchemaRepository productSchemaRepository;

    private String token;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        String userName = "depotuser";
        String userPassword = "depotuser";
        token = registerUserAndGetToken(userName, userPassword);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        productSchemaRepository.deleteAll();
        categoryRepository.deleteAll();
        supplierRepository.deleteAll();
        userRepository.deleteAll();
        inventoryService.clearStock();
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
        String actualUserName = userResponse.getUserName();
        Role actualRole = userResponse.getRole();
        LocalDateTime createdAt = userResponse.getCreatedAt();
        LocalDateTime updatedAt = userResponse.getUpdatedAt();

        assertNotNull(userResponse);
        assertEquals("depotuser", actualUserName);
        assertEquals(Role.USER, actualRole);
        TestUtil.assertCreatedAndUpdatedTimes(createdAt, updatedAt);
    }

    @Test
    void shouldFetchAllProductsSuccessfully() {
        //***** <-- given: Existing products in the repository --> *****//
        TestUtil.CreateProductResponse createProductResponse = TestUtil.createProduct(
                categoryRepository,
                productSchemaRepository,
                productRepository,
                List.of(new TestUtil.CategoryUnit("Category 1", "Test category")),
                "Test Product",
                10,
                "Test Product Description",
                1L,
                ProductStatus.FREE,
                LocalDateTime.now().plusDays(5)
        );
        Product savedProduct = createProductResponse.product();

        //***** <-- when: Make authenticated request to fetch all products --> *****//
        List<ProductResponse> productResponses = getAllProductsWithToken(token);

        //***** <-- then: Validate response properties --> *****//
        ProductResponse productResponse = productResponses.get(0);
        Long actualId = productResponse.getId();
        String actualProductName = productResponse.getProductName();
        String actualDescription = productResponse.getDescription();
        String actualStatus = productResponse.getStatus();
        List<String> actualCategories = productResponse.getCategories();

        assertNotNull(productResponses);
        assertEquals(1, productResponses.size());
        assertEquals(savedProduct.getId(), actualId);
        assertEquals("Test Product", actualProductName);
        assertEquals("Test Product Description", actualDescription);
        assertEquals(ProductStatus.FREE.name(), actualStatus);
        assertEquals(1, actualCategories.size());
        assertEquals("Category 1", actualCategories.get(0));
    }

    @Test
    void shouldReturnProductsForCategory() {
        //***** <-- given: Create category, product schema, and products --> *****//
        TestUtil.CategoryUnit categoryUnit = new TestUtil.CategoryUnit("Electronics", "Electronics category");
        TestUtil.CreateProductResponse createProductResponse1 = TestUtil.createProduct(
                categoryRepository,
                productSchemaRepository,
                productRepository,
                List.of(categoryUnit),
                "Smartphone",
                5,
                "High-end smartphone",
                1L,
                ProductStatus.FREE,
                LocalDateTime.now().plusDays(10)
        );

        TestUtil.CategoryUnit categoryUnit2 = new TestUtil.CategoryUnit("Store", "Store category");
        TestUtil.CreateProductResponse createProductResponse2 = TestUtil.createProduct(
                categoryRepository,
                productSchemaRepository,
                productRepository,
                List.of(categoryUnit2),
                "Laptop",
                10,
                "Gaming laptop",
                1L,
                ProductStatus.FREE,
                LocalDateTime.now().plusDays(15)
        );

        Long categoryId = createProductResponse1.categories().get(0).getId();

        //***** <-- when: Make authenticated request to fetch products for the category --> *****//
        List<ProductResponse> productResponses = webTestClient
                .get()
                .uri(INFO_USER_PRODUCTS_OF_CATEGORY_PATH + categoryId)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        //***** <-- then: Validate response properties --> *****//
        assertNotNull(productResponses);
        assertEquals(1, productResponses.size());

        ProductResponse productResponse1 = productResponses.get(0);

        assertEquals("Smartphone", productResponse1.getProductName());
        assertEquals("High-end smartphone", productResponse1.getDescription());
        assertTrue(productResponse1.getCategories().contains("Electronics"));
    }

    @Test
    void shouldFetchAllSuppliersSuccessfully() {
        //***** <-- given: Existing suppliers in the repository --> *****//
        TestUtil.CreateSupplierResponse createSupplierResponse =
                TestUtil.createSupplier(userRepository,
                        10,
                        5,
                        5,
                        10,
                        "Test Supplier",
                        "password",
                        passwordEncoder);

        Supplier savedSupplier = createSupplierResponse.supplier();

        //***** <-- when: Make authenticated request to fetch all suppliers --> *****//
        List<SupplierResponse> supplierResponses = getAllSuppliersWithToken(token);

        //***** <-- then: Validate response properties --> *****//
        SupplierResponse supplierResponse = supplierResponses.get(0);
        String actualName = supplierResponse.getName();

        assertNotNull(supplierResponses);
        assertEquals(1, supplierResponses.size());
        assertEquals(savedSupplier.getName(), actualName);
    }

    @Test
    void shouldFetchUserOrdersSuccessfully() {
        //***** <-- given: Existing orders for the user in the repository --> *****//
        String userName = "orderuser";
        String userPassword = "password";
        String userToken = registerUserAndGetToken(userName, userPassword);

        MyUser user = userRepository.findByUserName(userName).orElseThrow();

        TestUtil.CreateProductResponse createProductResponse = TestUtil.createProduct(
                categoryRepository,
                productSchemaRepository,
                productRepository,
                new ArrayList<>(),
                "Order Product",
                5,
                "Order Product Description",
                1L,
                ProductStatus.FREE,
                LocalDateTime.now().plusDays(10)
        );
        Product savedProduct = createProductResponse.product();

        Order order = Order.builder()
                .userId(user.getId())
                .orderItems(List.of(savedProduct))
                .status(OrderStatus.PENDING)
                .build();
        Order savedOrder = orderRepository.save(order);

        //***** <-- when: Make authenticated request to fetch user orders --> *****//
        List<OrderResponse> orderResponses = getUserOrdersWithToken(userToken);

        //***** <-- then: Validate response properties --> *****//
        OrderResponse orderResponse = orderResponses.get(0);
        Long actualOrderId = orderResponse.getId();
        String actualStatus = orderResponse.getStatus();
        OrderItemResponse orderItemResponse = orderResponse.getOrderItems().get(0);

        assertNotNull(orderResponses);
        assertEquals(1, orderResponses.size());
        assertEquals(savedOrder.getId(), actualOrderId);
        assertEquals(OrderStatus.PENDING.name(), actualStatus);
        assertEquals(savedProduct.getSchema().getName(), orderItemResponse.getProductName());
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

    private List<ProductResponse> getAllProductsWithToken(String token) {
        return webTestClient
                .get()
                .uri(INFO_USER_PRODUCT_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private List<SupplierResponse> getAllSuppliersWithToken(String token) {
        return webTestClient
                .get()
                .uri(INFO_USER_SUPPLIER_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SupplierResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private List<OrderResponse> getUserOrdersWithToken(String token) {
        return webTestClient
                .get()
                .uri(INFO_USER_OWN_ORDER_PATH)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OrderResponse.class)
                .returnResult()
                .getResponseBody();
    }
}
