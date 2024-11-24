package com.bme.vik.aut.thesis.depot.general.order;

import static org.junit.jupiter.api.Assertions.*;
import com.bme.vik.aut.thesis.depot.exception.inventory.DepotFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryOutOfStockException;
import com.bme.vik.aut.thesis.depot.exception.product.InvalidProductExpiryException;
import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.supplier.NonGreaterThanZeroQuantityException;
import com.bme.vik.aut.thesis.depot.exception.user.UserSupplierNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaService;
import com.bme.vik.aut.thesis.depot.general.alert.AlertService;
import com.bme.vik.aut.thesis.depot.general.alert.event.LowStockAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ProductExpiredAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ReorderAlertEvent;
import com.bme.vik.aut.thesis.depot.general.report.ReportService;
import com.bme.vik.aut.thesis.depot.general.report.dto.InventoryState;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryRepository;
import com.bme.vik.aut.thesis.depot.exception.category.CategoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.productschema.NonGreaterThanZeroStorageSpaceException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.user.UserNameAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.user.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.dto.CreateProductSchemaRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.product.*;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.ProductStockResponse;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.RemoveProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierRepository;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.CreateSupplierRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.general.util.TestUtil;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

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
