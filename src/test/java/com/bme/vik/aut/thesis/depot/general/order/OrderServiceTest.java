package com.bme.vik.aut.thesis.depot.general.order;

import static org.junit.jupiter.api.Assertions.*;
import com.bme.vik.aut.thesis.depot.exception.inventory.DepotFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryOutOfStockException;
import com.bme.vik.aut.thesis.depot.exception.order.NotOwnOrderException;
import com.bme.vik.aut.thesis.depot.exception.order.OrderNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.order.TooLargeOrderException;
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
import com.bme.vik.aut.thesis.depot.general.order.dto.COWProductId;
import com.bme.vik.aut.thesis.depot.general.order.dto.COWProductName;
import com.bme.vik.aut.thesis.depot.general.order.dto.COWProductSupplierName;
import com.bme.vik.aut.thesis.depot.general.order.dto.CreateOrderRequest;
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
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.SupplierService;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.CreateSupplierRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ProductService productService;

    @Mock
    private ProductSchemaService productSchemaService;

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldReturnAllOrdersSuccessfully() {
        //***** <-- given: Mock orders --> *****//
        Order order1 = Order.builder()
                .id(1L)
                .userId(100L)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order order2 = Order.builder()
                .id(2L)
                .userId(101L)
                .status(OrderStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        //***** <-- when: Fetch all orders --> *****//
        List<Order> orders = orderService.getAllOrders();

        //***** <-- then: Validate response --> *****//
        assertNotNull(orders);
        assertEquals(2, orders.size());
        assertEquals(1L, orders.get(0).getId());
        assertEquals(100L, orders.get(0).getUserId());
        assertEquals(OrderStatus.PENDING, orders.get(0).getStatus());
        assertEquals(2L, orders.get(1).getId());
        assertEquals(101L, orders.get(1).getUserId());
        assertEquals(OrderStatus.COMPLETED, orders.get(1).getStatus());

        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnOrderByIdSuccessfully() {
        //***** <-- given: Mock order --> *****//
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .userId(100L)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        //***** <-- when: Fetch order by ID --> *****//
        Order fetchedOrder = orderService.getOrderById(orderId);

        //***** <-- then: Validate response --> *****//
        assertNotNull(fetchedOrder);
        assertEquals(orderId, fetchedOrder.getId());
        assertEquals(100L, fetchedOrder.getUserId());
        assertEquals(OrderStatus.PENDING, fetchedOrder.getStatus());

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFoundById() {
        //***** <-- given: Non-existent order ID --> *****//
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to fetch non-existent order, expect exception --> *****//
        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId)
        );

        assertEquals("Order with ID 999 not found", exception.getMessage());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void shouldReturnAllPendingOrders() {
        //***** <-- given: Mock pending orders --> *****//
        Order pendingOrder1 = Order.builder()
                .id(1L)
                .userId(100L)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order pendingOrder2 = Order.builder()
                .id(2L)
                .userId(101L)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findAllByStatus(OrderStatus.PENDING)).thenReturn(List.of(pendingOrder1, pendingOrder2));

        //***** <-- when: Fetch all pending orders --> *****//
        List<Order> orders = orderService.getAllPendingOrders();

        //***** <-- then: Validate response --> *****//
        assertNotNull(orders);
        assertEquals(2, orders.size());
        assertEquals(1L, orders.get(0).getId());
        assertEquals(OrderStatus.PENDING, orders.get(0).getStatus());
        assertEquals(2L, orders.get(1).getId());
        assertEquals(OrderStatus.PENDING, orders.get(1).getStatus());

        verify(orderRepository, times(1)).findAllByStatus(OrderStatus.PENDING);
    }

    @Test
    void shouldReturnPendingOrderByIdSuccessfully() {
        //***** <-- given: Mock pending order --> *****//
        Long orderId = 1L;
        Order pendingOrder = Order.builder()
                .id(orderId)
                .userId(100L)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findByIdAndStatus(orderId, OrderStatus.PENDING)).thenReturn(Optional.of(pendingOrder));

        //***** <-- when: Fetch pending order by ID --> *****//
        Order fetchedOrder = orderService.getPendingOrderById(orderId);

        //***** <-- then: Validate response --> *****//
        assertNotNull(fetchedOrder);
        assertEquals(orderId, fetchedOrder.getId());
        assertEquals(OrderStatus.PENDING, fetchedOrder.getStatus());

        verify(orderRepository, times(1)).findByIdAndStatus(orderId, OrderStatus.PENDING);
    }

    @Test
    void shouldThrowExceptionWhenPendingOrderNotFoundById() {
        //***** <-- given: Non-existent or non-pending order ID --> *****//
        Long orderId = 999L;
        when(orderRepository.findByIdAndStatus(orderId, OrderStatus.PENDING)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to fetch non-existent or non-pending order, expect exception --> *****//
        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getPendingOrderById(orderId)
        );

        assertEquals("Order with ID 999 not found or not in pending state", exception.getMessage());
        verify(orderRepository, times(1)).findByIdAndStatus(orderId, OrderStatus.PENDING);
    }

    @Test
    void shouldApprovePendingOrderSuccessfully() {
        //***** <-- given: Mock pending order --> *****//
        Long orderId = 1L;
        Order pendingOrder = Order.builder()
                .id(orderId)
                .userId(100L)
                .status(OrderStatus.PENDING)
                .orderItems(List.of()) // Mock empty order items for simplicity
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findByIdAndStatus(orderId, OrderStatus.PENDING)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //***** <-- when: Approve the pending order --> *****//
        Order approvedOrder = orderService.approvePendingOrder(orderId);

        //***** <-- then: Validate order status and interactions --> *****//
        assertNotNull(approvedOrder);
        assertEquals(orderId, approvedOrder.getId());
        assertEquals(OrderStatus.COMPLETED, approvedOrder.getStatus());

        verify(orderRepository, times(1)).findByIdAndStatus(orderId, OrderStatus.PENDING);
        verify(orderRepository, times(1)).save(pendingOrder);
        verify(inventoryService, times(1)).removeCompletedOrderProducts(pendingOrder.getOrderItems());
    }

    @Nested
    class CancelOrder {

        @Test
        void shouldCancelOrderSuccessfullyWhenUserIsAdmin() {
            //***** <-- given: Admin user and a pending order --> *****//
            Long orderId = 1L;
            MyUser adminUser = MyUser.builder()
                    .id(1L)
                    .userName("admin")
                    .role(Role.ADMIN)
                    .build();

            Order pendingOrder = Order.builder()
                    .id(orderId)
                    .userId(2L) // Order owned by a different user
                    .status(OrderStatus.PENDING)
                    .orderItems(List.of())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

            //***** <-- when: Admin cancels the order --> *****//
            orderService.cancelOrder(orderId, adminUser);

            //***** <-- then: Validate order status and interactions --> *****//
            assertEquals(OrderStatus.CANCELLED, pendingOrder.getStatus());
            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryService, times(1)).freeProducts(pendingOrder.getOrderItems());
            verify(orderRepository, times(1)).save(pendingOrder);
        }

        @Test
        void shouldThrowExceptionWhenUserTriesToCancelNotOwnOrder() {
            //***** <-- given: Non-admin user and another user's order --> *****//
            Long orderId = 1L;
            MyUser regularUser = MyUser.builder()
                    .id(1L)
                    .userName("user")
                    .role(Role.USER)
                    .build();

            Order otherUsersOrder = Order.builder()
                    .id(orderId)
                    .userId(2L) // Order owned by a different user
                    .status(OrderStatus.PENDING)
                    .orderItems(List.of())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(otherUsersOrder));

            //***** <-- when & then: Attempt to cancel another user's order, expect exception --> *****//
            NotOwnOrderException exception = assertThrows(
                    NotOwnOrderException.class,
                    () -> orderService.cancelOrder(orderId, regularUser)
            );

            assertEquals("User does not own this order with ID: " + orderId, exception.getMessage());
            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryService, never()).freeProducts(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldCancelOwnOrderSuccessfullyWhenUserIsOwner() {
            //***** <-- given: Regular user and their pending order --> *****//
            Long orderId = 1L;
            MyUser regularUser = MyUser.builder()
                    .id(1L)
                    .userName("user")
                    .role(Role.USER)
                    .build();

            Order ownPendingOrder = Order.builder()
                    .id(orderId)
                    .userId(1L) // Order owned by the user
                    .status(OrderStatus.PENDING)
                    .orderItems(List.of())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(ownPendingOrder));

            //***** <-- when: User cancels their own order --> *****//
            orderService.cancelOrder(orderId, regularUser);

            //***** <-- then: Validate order status and interactions --> *****//
            assertEquals(OrderStatus.CANCELLED, ownPendingOrder.getStatus());
            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryService, times(1)).freeProducts(ownPendingOrder.getOrderItems());
            verify(orderRepository, times(1)).save(ownPendingOrder);
        }
    }

    @Nested
    class CreateOrderTests {

        @Test
        void shouldCreateOrderSuccessfullyWithMixedRequests() {
            //***** <-- given: Product schemas, inventories, and requests --> *****//
            MyUser user = MyUser.builder().id(1L).userName("testUser").build();

            ProductSchema schema1 = ProductSchema.builder().id(1L).name("Product A").build();
            ProductSchema schema2 = ProductSchema.builder().id(2L).name("Product B").build();

            Product product1 = Product.builder().id(10L).schema(schema1).build();
            Product product2 = Product.builder().id(20L).schema(schema2).build();

            Inventory inventory = Inventory.builder().id(1L).build();

            Supplier supplier = Supplier.builder().id(1L).name("Test Supplier").build();
            supplier.setInventory(inventory);

            // Requests
            List<CreateOrderRequest> requests = List.of(
                    new COWProductName("Product A", 1),
                    new COWProductSupplierName("Test Supplier","Product B", 1),
                    new COWProductId(product1.getId())
            );

            // Mock behaviors
            when(productSchemaService.getProductSchemaByName("Product A")).thenReturn(schema1);
            when(inventoryService.getInventoriesWithStockForSchema(schema1.getId())).thenReturn(List.of(inventory));
            when(productSchemaService.getProductSchemaByName("Product B")).thenReturn(schema2);
            when(productService.getProductById(product1.getId())).thenReturn(product1);
            when(productService.isProductAvailable(product1)).thenReturn(true);
            when(inventoryService.getCurrentStockBySchemaId(schema1.getId())).thenReturn(10);
            when(inventoryService.getCurrentStockBySchemaId(schema2.getId())).thenReturn(10);

            when(inventoryService.reserveProdByProdName(any(Inventory.class), eq(schema1), anyInt())).thenReturn(List.of(product1));
            when(supplierService.getSupplierByName("Test Supplier")).thenReturn(supplier);
            when(inventoryService.reserveProdByProdSupplName(any(Inventory.class), eq(schema2), anyInt())).thenReturn(List.of(product2));
            when(inventoryService.getByProductId(product1.getId())).thenReturn(inventory);
            doNothing().when(inventoryService).reserveOneProduct(any(Inventory.class), eq(product1));

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            //***** <-- when: Create order --> *****//
            Order createdOrder = orderService.createOrder(user, requests);

            //***** <-- then: Verify order details --> *****//
            assertNotNull(createdOrder);
            assertEquals(OrderStatus.PENDING, createdOrder.getStatus());
            assertEquals(3, createdOrder.getOrderItems().size());
            verify(orderRepository, times(1)).save(createdOrder);
        }

        @Test
        void shouldThrowTooLargeOrderExceptionWhenStockIsInsufficient() {
            //***** <-- given: Insufficient stock for a product --> *****//
            MyUser user = MyUser.builder().id(1L).userName("testUser").build();

            ProductSchema schema1 = ProductSchema.builder().id(1L).name("Product A").build();

            // Requests
            List<CreateOrderRequest> requests = List.of(new COWProductName("Product A", 15));

            // Mock behaviors
            when(productSchemaService.getProductSchemaByName("Product A")).thenReturn(schema1);
            when(inventoryService.getCurrentStockBySchemaId(schema1.getId())).thenReturn(10);

            //***** <-- when & then: Attempt to create order, expect exception --> *****//
            TooLargeOrderException exception = assertThrows(
                    TooLargeOrderException.class,
                    () -> orderService.createOrder(user, requests)
            );

            assertEquals(
                    "Not enough stock available for product schema ID " + schema1.getId() + ". Required: 15, Available: 10",
                    exception.getMessage()
            );
            verify(orderRepository, never()).save(any());
        }
    }
}
