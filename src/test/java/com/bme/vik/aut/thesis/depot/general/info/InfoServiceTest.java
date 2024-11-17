package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
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
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfoServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InfoService infoService;

    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        // Use the actual ModelMapper
        modelMapper = new ModelMapper();
        infoService = new InfoService(userRepository, modelMapper, productRepository, supplierRepository, orderRepository);
    }

    @Test
    void shouldFetchUserInfoSuccessfully() {
        //***** <-- given: Existing user in the repository --> *****//
        String username = "existingUser";
        Long userId = 1L;
        Role role = Role.USER;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        MyUser mockUser = MyUser.builder()
                .id(userId)
                .userName(username)
                .role(role)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        when(userRepository.findByUserName(username)).thenReturn(Optional.of(mockUser));

        //***** <-- when: Fetch user info --> *****//
        UserResponse actualResponse = infoService.getUserInfoByName(username);

        //***** <-- then: Validate response --> *****//
        assertNotNull(actualResponse);
        assertEquals(username, actualResponse.getUserName());
        assertEquals(role, actualResponse.getRole());
        assertEquals(createdAt, actualResponse.getCreatedAt());
        assertEquals(updatedAt, actualResponse.getUpdatedAt());

        verify(userRepository).findByUserName(username);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        //***** <-- given: Non-existing user in the repository --> *****//
        String username = "nonExistentUser";

        when(userRepository.findByUserName(username)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to fetch user info, expect exception --> *****//
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class, () -> infoService.getUserInfoByName(username)
        );

        assertEquals("User not found with username: " + username, exception.getMessage());
        verify(userRepository).findByUserName(username);
    }

    @Test
    void shouldFetchAllSuppliers() {
        //***** <-- given: Existing suppliers in the repository --> *****//
        Long supplier1Id = 1L;
        String supplier1Name = "Supplier 1";
        Long supplier2Id = 2L;
        String supplier2Name = "Supplier 2";

        Supplier supplier1 = Supplier.builder()
                .id(supplier1Id)
                .name(supplier1Name)
                .build();

        Supplier supplier2 = Supplier.builder()
                .id(supplier2Id)
                .name(supplier2Name)
                .build();

        List<Supplier> mockSuppliers = new ArrayList<>(List.of(supplier1, supplier2));
        when(supplierRepository.findAll()).thenReturn(mockSuppliers);

        //***** <-- when: Fetch all suppliers --> *****//
        List<SupplierResponse> actualResponses = infoService.getAllSuppliers();

        //***** <-- then: Validate responses --> *****//
        assertNotNull(actualResponses);

        SupplierResponse actualSupplier1 = actualResponses.get(0);
        SupplierResponse actualSupplier2 = actualResponses.get(1);

        assertEquals(supplier1Id, actualSupplier1.getId());
        assertEquals(supplier1Name, actualSupplier1.getName());
        assertEquals(supplier2Id, actualSupplier2.getId());
        assertEquals(supplier2Name, actualSupplier2.getName());

        verify(supplierRepository).findAll();
    }

    @Test
    void shouldFetchAllProducts() {
        //***** <-- given: Existing products in the repository --> *****//
        Long schemaId = 1L;
        String schemaName = "Product Schema";
        String categoryName = "Category 1";
        String productDescription = "Sample Product";
        ProductStatus productStatus = ProductStatus.FREE;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(5);

        Category category1 = Category.builder()
                .name(categoryName)
                .description("Test")
                .build();

        ProductSchema schema = ProductSchema.builder()
                .id(schemaId)
                .name(schemaName)
                .categories(List.of(category1))
                .build();

        Product product = Product.builder()
                .id(1L)
                .schema(schema)
                .description(productDescription)
                .status(productStatus)
                .expiresAt(expiresAt)
                .build();

        List<Product> mockProducts = List.of(product);
        when(productRepository.findAll()).thenReturn(mockProducts);

        //***** <-- when: Fetch all products --> *****//
        List<ProductResponse> actualResponses = infoService.getAllProducts();

        //***** <-- then: Validate responses --> *****//
        ProductResponse actualProduct = actualResponses.get(0);

        assertNotNull(actualResponses);
        assertEquals(1, actualResponses.size());
        assertEquals(schemaName, actualProduct.getProductName());
        assertEquals(productDescription, actualProduct.getDescription());
        assertEquals(categoryName, actualProduct.getCategories().get(0));
        assertEquals(productStatus.name(), actualProduct.getStatus());
        assertEquals(expiresAt, actualProduct.getExpiresAt());

        verify(productRepository).findAll();
    }

    @Test
    void shouldFetchAllUserOrders() {
        //***** <-- given: Existing orders in the repository --> *****//
        Long userId1 = 1L;
        Long schemaId = 1L;
        String schemaName = "Product Schema";
        Long productId = 1L;

        ProductSchema schema = ProductSchema.builder()
                .id(schemaId)
                .name(schemaName)
                .build();

        Product product = Product.builder()
                .id(productId)
                .schema(schema)
                .supplierId(1L)
                .build();

        Order orderForUser1 = Order.builder()
                .id(1L)
                .userId(userId1)
                .orderItems(List.of(product))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order orderForUser2 = Order.builder()
                .id(2L)
                .userId(userId1)
                .orderItems(List.of(product))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findAllByUserId(userId1)).thenReturn(List.of(orderForUser1, orderForUser2));

        //***** <-- when: Fetch user 1 orders --> *****//
        List<OrderResponse> actualResponses = infoService.getUserOrders(userId1);

        //***** <-- then: Validate responses --> *****//
        assertNotNull(actualResponses);
        assertEquals(2, actualResponses.size());

        OrderResponse actualOrder1 = actualResponses.get(0);
        OrderResponse actualOrder2 = actualResponses.get(1);

        assertEquals(orderForUser1.getId(), actualOrder1.getId());
        assertEquals(schemaName, actualOrder1.getOrderItems().get(0).getProductName());
        assertEquals(productId, actualOrder1.getOrderItems().get(0).getProductId());
        assertEquals(OrderStatus.PENDING.name(), actualOrder1.getStatus());

        assertEquals(orderForUser2.getId(), actualOrder2.getId());
        assertEquals(schemaName, actualOrder2.getOrderItems().get(0).getProductName());
        assertEquals(productId, actualOrder2.getOrderItems().get(0).getProductId());
        assertEquals(OrderStatus.PENDING.name(), actualOrder2.getStatus());

        verify(orderRepository).findAllByUserId(userId1);
    }
}
