package com.bme.vik.aut.thesis.depot.general.supplier.product;

import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchemaService;
import com.bme.vik.aut.thesis.depot.general.alert.event.LowStockAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ProductExpiredAlertEvent;
import com.bme.vik.aut.thesis.depot.general.alert.event.ReorderAlertEvent;
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
import com.bme.vik.aut.thesis.depot.general.supplier.product.ExpiryStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductStatus;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.Supplier;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserModifyRequest;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldFetchProductByIdSuccessfully() {
        //***** <-- given: Prepare test data --> *****//

        Long productId = 1L;
        ProductStatus productStatus = ProductStatus.FREE;
        ExpiryStatus expiryStatus = ExpiryStatus.NOTEXPIRED;
        String productDescription = "Test Product";
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        ProductSchema schema = ProductSchema.builder().id(100L).name("Test Schema").build();
        Long supplierId = 50L;

        Product product = Product.builder()
                .id(productId)
                .schema(schema)
                .supplierId(supplierId)
                .description(productDescription)
                .status(productStatus)
                .expiryStatus(expiryStatus)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        //***** <-- when: Fetch product by ID --> *****//
        Product fetchedProduct = productService.getProductById(productId);

        //***** <-- then: Verify fetched product --> *****//
        assertEquals(productId, fetchedProduct.getId());
        assertEquals(schema, fetchedProduct.getSchema());
        assertEquals(supplierId, fetchedProduct.getSupplierId());
        assertEquals(productDescription, fetchedProduct.getDescription());
        assertEquals(productStatus, fetchedProduct.getStatus());
        assertEquals(expiryStatus, fetchedProduct.getExpiryStatus());
        assertEquals(expiresAt, fetchedProduct.getExpiresAt());
        assertEquals(createdAt, fetchedProduct.getCreatedAt());
        assertEquals(updatedAt, fetchedProduct.getUpdatedAt());
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        //***** <-- given: Prepare test data --> *****//
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        //***** <-- when & then: Verify exception is thrown --> *****//
        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(productId));
    }

    @Test
    void shouldCorrectlyDetermineIfProductIsAvailable() {
        //***** <-- given: Prepare test data --> *****//
        ProductSchema schema = ProductSchema.builder().id(100L).name("Test Schema").build();
        Long supplierId = 50L;
        Product product = Product.builder()
                .id(1L)
                .schema(schema)
                .supplierId(supplierId)
                .description("Test Product")
                .status(ProductStatus.FREE)
                .build();

        //***** <-- when & then: Verify product availability --> *****//
        assertTrue(productService.isProductAvailable(product));

        //***** <-- given: Modify product status --> *****//
        product.setStatus(ProductStatus.RESERVED);

        //***** <-- when & then: Verify product is unavailable --> *****//
        assertFalse(productService.isProductAvailable(product));
    }
}
