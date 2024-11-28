package com.bme.vik.aut.thesis.depot.general.order;

import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import com.bme.vik.aut.thesis.depot.general.util.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductSchemaRepository productSchemaRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create Category, ProductSchema, and Product
        TestUtil.CategoryUnit categoryUnit = new TestUtil.CategoryUnit("Electronics", "Electronics category");
        TestUtil.CreateProductResponse productResponse = TestUtil.createProduct(
                categoryRepository,
                productSchemaRepository,
                productRepository,
                List.of(categoryUnit),
                "Test Product",
                10,
                "Test Description",
                1L,
                ProductStatus.FREE,
                LocalDateTime.now().plusDays(10)
        );

        testProduct = productResponse.product();
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        productSchemaRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void shouldFindAllOrdersByUserId() {
        // Create orders
        Order order1 = TestUtil.createOrder(orderRepository, List.of(testProduct), OrderStatus.PENDING, 1L);
        Order order2 = TestUtil.createOrder(orderRepository, List.of(testProduct), OrderStatus.PENDING, 2L);

        // Fetch orders by user ID
        List<Order> user1Orders = orderRepository.findAllByUserId(1L);

        // Validate
        assertEquals(1, user1Orders.size());
        assertEquals(order1.getId(), user1Orders.get(0).getId());
    }

    @Test
    void shouldReturnEmptyWhenNoOrdersForUserId() {
        // Fetch orders for a non-existent user ID
        List<Order> userOrders = orderRepository.findAllByUserId(99L);

        // Validate
        assertTrue(userOrders.isEmpty());
    }

    @Test
    void shouldFindAllOrdersByStatus() {
        // Create orders
        TestUtil.createOrder(orderRepository, List.of(testProduct), OrderStatus.PENDING, 1L);
        TestUtil.createOrder(orderRepository, List.of(testProduct), OrderStatus.COMPLETED, 1L);

        // Fetch orders by status
        List<Order> pendingOrders = orderRepository.findAllByStatus(OrderStatus.PENDING);

        // Validate
        assertEquals(1, pendingOrders.size());
        assertEquals(OrderStatus.PENDING, pendingOrders.get(0).getStatus());
    }

    @Test
    void shouldFindOrderByIdAndStatus() {
        // Create order
        Order order = TestUtil.createOrder(orderRepository, List.of(testProduct), OrderStatus.PENDING, 1L);

        // Fetch order by ID and status
        Optional<Order> foundOrder = orderRepository.findByIdAndStatus(order.getId(), OrderStatus.PENDING);

        // Validate
        assertTrue(foundOrder.isPresent());
        assertEquals(order.getId(), foundOrder.get().getId());
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFoundByIdAndStatus() {
        // Fetch order by non-existent ID and status
        Optional<Order> foundOrder = orderRepository.findByIdAndStatus(99L, OrderStatus.COMPLETED);

        // Validate
        assertFalse(foundOrder.isPresent());
    }
}
