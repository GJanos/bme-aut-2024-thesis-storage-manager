package com.bme.vik.aut.thesis.depot.general.supplier.product;

import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
