package com.bme.vik.aut.thesis.depot.general.admin.productschema;

import com.bme.vik.aut.thesis.depot.exception.category.CategoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.productschema.NonGreaterThanZeroStorageSpaceException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.dto.CreateProductSchemaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSchemaServiceTest {

    @Mock
    private ProductSchemaRepository productSchemaRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductSchemaService productSchemaService;

    @Test
    void shouldReturnEmptyPSListWhenNoProductSchemasExist() {
        //***** <-- given: No product schemas in repository --> *****//
        when(productSchemaRepository.findAll()).thenReturn(Collections.emptyList());

        //***** <-- when: Retrieve all product schemas --> *****//
        List<ProductSchema> productSchemas = productSchemaService.getAllProductSchemas();

        //***** <-- then: Expect empty list --> *****//
        assertTrue(productSchemas.isEmpty(), "Product schema list should be empty when none exist");
        verify(productSchemaRepository).findAll();
    }

    @Test
    void shouldReturnAllPSWhenMultiplePSExist() {
        //***** <-- given: Multiple product schemas in repository --> *****//
        List<ProductSchema> mockProductSchemas = List.of(
                ProductSchema.builder().id(1L).name("Product1").storageSpaceNeeded(10).build(),
                ProductSchema.builder().id(2L).name("Product2").storageSpaceNeeded(15).build()
        );
        when(productSchemaRepository.findAll()).thenReturn(mockProductSchemas);

        //***** <-- when: Retrieve all product schemas --> *****//
        List<ProductSchema> productSchemas = productSchemaService.getAllProductSchemas();

        //***** <-- then: Expect list of product schemas --> *****//
        assertEquals(2, productSchemas.size(), "Product schema list should contain two entries");
        verify(productSchemaRepository).findAll();
    }

    @Test
    void shouldReturnPSByIdWhenValidIdProvided() {
        //***** <-- given: Product schema with valid ID --> *****//
        Long productId = 1L;
        ProductSchema mockProductSchema = ProductSchema.builder()
                .id(productId)
                .name("Product1")
                .storageSpaceNeeded(10)
                .build();
        when(productSchemaRepository.findById(productId)).thenReturn(Optional.of(mockProductSchema));

        //***** <-- when: Retrieve product schema by ID --> *****//
        ProductSchema productSchema = productSchemaService.getProductSchemaById(productId);

        //***** <-- then: Verify returned product schema --> *****//
        assertNotNull(productSchema);
        assertEquals("Product1", productSchema.getName());
        assertEquals(10, productSchema.getStorageSpaceNeeded());
        verify(productSchemaRepository).findById(productId);
    }

    @Test
    void shouldThrowExceptionWhenRequestingPSAndInvalidIdProvided() {
        //***** <-- given: Invalid product schema ID --> *****//
        Long invalidId = 999L;
        when(productSchemaRepository.findById(invalidId)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to retrieve product schema by invalid ID --> *****//
        assertThrows(ProductSchemaNotFoundException.class, () -> productSchemaService.getProductSchemaById(invalidId));
        verify(productSchemaRepository).findById(invalidId);
    }

    @Test
    void shouldCreatePSWhenValidDetailsProvided() {
        //***** <-- given: Valid product schema creation request --> *****//
        CreateProductSchemaRequest request = CreateProductSchemaRequest.builder()
                .name("NewProduct")
                .storageSpaceNeeded(20)
                .categoryIDs(List.of(1L, 2L))
                .build();

        List<Category> mockCategories = List.of(
                Category.builder().id(1L).name("Category1").build(),
                Category.builder().id(2L).name("Category2").build()
        );

        when(categoryRepository.findAllById(request.getCategoryIDs())).thenReturn(mockCategories);
        when(productSchemaRepository.existsByName(request.getName())).thenReturn(false);

        when(productSchemaRepository.save(any(ProductSchema.class)))
                .thenAnswer(invocation -> {
                    ProductSchema productSchema = invocation.getArgument(0);
                    productSchema.setId(1L); // Assign ID as a database would
                    return productSchema;
                });

        //***** <-- when: Create product schema --> *****//
        ProductSchema createdSchema = productSchemaService.createProductSchema(request);

        //***** <-- then: Verify creation --> *****//
        assertNotNull(createdSchema);
        assertEquals(1L, createdSchema.getId());
        assertEquals("NewProduct", createdSchema.getName());
        assertEquals(20, createdSchema.getStorageSpaceNeeded());
        assertEquals(2, createdSchema.getCategories().size());
        assertEquals("Category1", createdSchema.getCategories().get(0).getName());
        assertEquals("Category2", createdSchema.getCategories().get(1).getName());

        verify(productSchemaRepository).save(any(ProductSchema.class));
    }


    @Test
    void shouldThrowExceptionWhenCreatingPSWithNonGTZeroStorageSpace() {
        //***** <-- given: Request with negative storage space --> *****//
        CreateProductSchemaRequest request = CreateProductSchemaRequest.builder()
                .name("ProductNegativeSpace")
                .storageSpaceNeeded(-5)
                .build();

        //***** <-- when & then: Attempt to create product schema with invalid storage space --> *****//
        assertThrows(NonGreaterThanZeroStorageSpaceException.class, () -> productSchemaService.createProductSchema(request));
        verify(productSchemaRepository, never()).save(any(ProductSchema.class));
    }

    @Test
    void shouldNotCreatePSWhenNameAlreadyExists() {
        //***** <-- given: Request with an already existing name --> *****//
        CreateProductSchemaRequest request = CreateProductSchemaRequest.builder()
                .name("ExistingProduct")
                .storageSpaceNeeded(10)
                .categoryIDs(List.of(1L))
                .build();

        when(productSchemaRepository.existsByName(request.getName())).thenReturn(true);

        //***** <-- when & then: Attempt to create product schema with duplicate name --> *****//
        assertThrows(ProductSchemaAlreadyExistsException.class, () -> productSchemaService.createProductSchema(request));
        verify(productSchemaRepository, never()).save(any(ProductSchema.class));
    }

    @Test
    void shouldNotCreatePSWhenInvalidCategoryIdsProvided() {
        //***** <-- given: Request with invalid category IDs --> *****//
        CreateProductSchemaRequest request = CreateProductSchemaRequest.builder()
                .name("NewProduct")
                .storageSpaceNeeded(15)
                .categoryIDs(List.of(1L, 99L)) // 99L doesn't exist
                .build();

        List<Category> mockCategories = List.of(
                Category.builder().id(1L).name("Category1").build()
        ); // Only one valid category returned

        when(categoryRepository.findAllById(request.getCategoryIDs())).thenReturn(mockCategories);

        //***** <-- when & then: Attempt to create product schema with invalid category IDs --> *****//
        assertThrows(CategoryNotFoundException.class, () -> productSchemaService.createProductSchema(request));
        verify(productSchemaRepository, never()).save(any(ProductSchema.class));
    }

    @Test
    void shouldUpdatePSSuccessfullyWhenValidDetailsProvided() {
        //***** <-- given: Existing product schema and valid update request --> *****//
        Long productId = 1L;
        CreateProductSchemaRequest request = CreateProductSchemaRequest.builder()
                .name("UpdatedProduct")
                .storageSpaceNeeded(25)
                .categoryIDs(List.of(1L))
                .build();

        ProductSchema existingSchema = ProductSchema.builder()
                .name("UpdatedProduct")
                .storageSpaceNeeded(15)
                .categories(new ArrayList<>())
                .build();

        List<Category> mockCategories = List.of(Category.builder().id(1L).name("Category1").build());

        when(productSchemaRepository.findById(productId)).thenReturn(Optional.of(existingSchema));
        when(productSchemaRepository.existsByName(request.getName())).thenReturn(true);
        when(categoryRepository.findAllById(request.getCategoryIDs())).thenReturn(mockCategories);
        when(productSchemaRepository.save(any(ProductSchema.class)))
                .thenAnswer(invocation -> {
                    ProductSchema productSchema = invocation.getArgument(0);
                    productSchema.setId(1L); // Assign ID as a database would
                    return productSchema;
                });

        //***** <-- when: Update product schema --> *****//
        ProductSchema updatedSchema = productSchemaService.updateProductSchema(productId, request);

        //***** <-- then: Verify update --> *****//
        assertNotNull(updatedSchema);
        assertEquals(1L, updatedSchema.getId());
        assertEquals("UpdatedProduct", updatedSchema.getName());
        assertEquals(25, updatedSchema.getStorageSpaceNeeded());
        assertEquals(1, updatedSchema.getCategories().size());
        verify(productSchemaRepository).save(any(ProductSchema.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingPSWithInvalidId() {
        //***** <-- given: Invalid product schema ID --> *****//
        Long invalidId = 999L;
        CreateProductSchemaRequest request = CreateProductSchemaRequest.builder()
                .name("UpdatedProduct")
                .storageSpaceNeeded(10)
                .categoryIDs(List.of(1L))
                .build();

        when(productSchemaRepository.findById(invalidId)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to update with invalid ID --> *****//
        assertThrows(ProductSchemaNotFoundException.class, () -> productSchemaService.updateProductSchema(invalidId, request));
        verify(productSchemaRepository).findById(invalidId);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingPSWithAlreadyExistingName() {
        //***** <-- given: Existing name in another product schema --> *****//
        Long productId = 1L;
        CreateProductSchemaRequest request = CreateProductSchemaRequest.builder()
                .name("ExistingProduct")
                .storageSpaceNeeded(20)
                .categoryIDs(List.of(1L))
                .build();

        ProductSchema existingSchema = ProductSchema.builder()
                .id(productId)
                .name("OriginalProduct")
                .storageSpaceNeeded(15)
                .build();

        when(productSchemaRepository.findById(productId)).thenReturn(Optional.of(existingSchema));
        when(productSchemaRepository.existsByName(request.getName())).thenReturn(true);

        //***** <-- when & then: Attempt to update with duplicate name --> *****//
        assertThrows(ProductSchemaAlreadyExistsException.class, () -> productSchemaService.updateProductSchema(productId, request));
        verify(productSchemaRepository, never()).save(any(ProductSchema.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingPSWithNonGTZeroStorageSpace() {
        //***** <-- given: Existing product schema and request with invalid storage space --> *****//
        Long productId = 1L;
        ProductSchema existingSchema = ProductSchema.builder()
                .id(productId)
                .name("ExistingProduct")
                .storageSpaceNeeded(10)
                .categories(new ArrayList<>())
                .build();

        CreateProductSchemaRequest request = CreateProductSchemaRequest.builder()
                .name("UpdatedProduct")
                .storageSpaceNeeded(-5) // Invalid storage space
                .categoryIDs(List.of(1L))
                .build();

        when(productSchemaRepository.findById(productId)).thenReturn(Optional.of(existingSchema));
        when(productSchemaRepository.existsByName(request.getName())).thenReturn(false);
        when(categoryRepository.findAllById(request.getCategoryIDs())).thenReturn(List.of(Category.builder().id(1L).name("Category1").build()));

        //***** <-- when & then: Attempt to update with non-positive storage space --> *****//
        assertThrows(NonGreaterThanZeroStorageSpaceException.class, () -> productSchemaService.updateProductSchema(productId, request));

        verify(productSchemaRepository).findById(productId);
        verify(productSchemaRepository, never()).save(any(ProductSchema.class));
    }

    @Test
    void shouldDeleteUserByIdWhenValidIdProvided() {
        //***** <-- given: Existing product schema ID --> *****//
        Long productId = 1L;
        ProductSchema existingSchema = ProductSchema.builder()
                .id(productId)
                .name("ProductToDelete")
                .build();

        when(productSchemaRepository.findById(productId)).thenReturn(Optional.of(existingSchema));
        doNothing().when(productSchemaRepository).delete(existingSchema);

        //***** <-- when: Delete product schema by ID --> *****//
        productSchemaService.deleteProductSchema(productId);

        //***** <-- then: Verify deletion --> *****//
        verify(productSchemaRepository).findById(productId);
        verify(productSchemaRepository).delete(existingSchema);
    }

    @Test
    void shouldThrowExceptionWhenDeletingUserWithInvalidId() {
        //***** <-- given: Invalid product schema ID --> *****//
        Long invalidId = 999L;
        when(productSchemaRepository.findById(invalidId)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to delete with invalid ID --> *****//
        assertThrows(ProductSchemaNotFoundException.class, () -> productSchemaService.deleteProductSchema(invalidId));
        verify(productSchemaRepository).findById(invalidId);
        verify(productSchemaRepository, never()).delete(any());
    }
}
