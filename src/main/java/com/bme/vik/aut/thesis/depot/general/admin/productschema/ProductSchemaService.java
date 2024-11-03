package com.bme.vik.aut.thesis.depot.general.admin.productschema;

import com.bme.vik.aut.thesis.depot.exception.category.CategoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.productschema.NonGreaterThanZeroStorageSpaceException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.category.Category;
import com.bme.vik.aut.thesis.depot.general.admin.category.CategoryRepository;
import com.bme.vik.aut.thesis.depot.general.admin.productschema.dto.CreateProductSchemaRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSchemaService {

    private final ProductSchemaRepository productSchemaRepository;
    private final CategoryRepository categoryRepository;
    private static final Logger logger = LoggerFactory.getLogger(ProductSchemaService.class);

    public List<ProductSchema> getAllProductSchemas() {
        logger.info("Fetching all product schemas");
        return productSchemaRepository.findAll();
    }

    public ProductSchema getProductSchemaById(Long id) {
        logger.info("Fetching product schema by ID: {}", id);
        return productSchemaRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Product schema with ID {} not found", id);
                    return new ProductSchemaNotFoundException("Product schema with ID " + id + " not found");
                });
    }

    public ProductSchema createProductSchema(CreateProductSchemaRequest request) {
        logger.info("Creating new product schema with name: {}", request.getName());

        checkIfProductSchemaExists(request);
        List<Category> categories = getProductSchemaCategories(request);

        ProductSchema productSchema = ProductSchema.builder()
                .name(request.getName())
                .storageSpaceNeeded(request.getStorageSpaceNeeded())
                .categories(categories)
                .build();

        ProductSchema savedSchema = productSchemaRepository.save(productSchema);
        logger.info("Product schema with ID {} created successfully", savedSchema.getId());

        return savedSchema;
    }

    public ProductSchema updateProductSchema(Long id, CreateProductSchemaRequest request) {
        logger.info("Updating product schema with ID: {}", id);

        ProductSchema productSchema = checkIfProductSchemaExistsAndFindIt(id, request);
        List<Category> categories = getProductSchemaCategories(request);

        productSchema.setName(request.getName());
        productSchema.setStorageSpaceNeeded(request.getStorageSpaceNeeded());
        productSchema.setCategories(categories);

        ProductSchema updatedSchema = productSchemaRepository.save(productSchema);
        logger.info("Product schema with ID {} updated successfully", updatedSchema.getId());

        return updatedSchema;
    }

    public void deleteProductSchema(Long id) {
        logger.info("Deleting product schema with ID: {}", id);
        ProductSchema productSchema = getProductSchemaById(id);
        productSchemaRepository.delete(productSchema);
        logger.info("Product schema with ID {} deleted successfully", id);
    }

    private void checkIfProductSchemaExists(CreateProductSchemaRequest request) {
        if (productSchemaRepository.existsByName(request.getName())) {
            logger.warn("Product schema with name {} already exists", request.getName());
            throw new ProductSchemaAlreadyExistsException("Product schema with name " + request.getName() + " already exists");
        }
    }

    private ProductSchema checkIfProductSchemaExistsAndFindIt(Long id, CreateProductSchemaRequest request) {
        ProductSchema productSchema = getProductSchemaById(id);

        if (productSchemaRepository.existsByName(request.getName()) && !productSchema.getName().equals(request.getName())) {
            logger.warn("Product schema with name {} already exists", request.getName());
            throw new ProductSchemaAlreadyExistsException("Product schema with name " + request.getName() + " already exists");
        }
        return productSchema;
    }

    private List<Category> getProductSchemaCategories(CreateProductSchemaRequest request) {
        if (request.getCategoryIDs() == null || request.getCategoryIDs().isEmpty()) {
            return new ArrayList<>();
        }

        List<Category> categories = categoryRepository.findAllById(request.getCategoryIDs());

        if (categories.size() != request.getCategoryIDs().size()) {
            throw new CategoryNotFoundException("One or more categories not found with provided IDs");
        }

        logger.info("Loaded {} categories for the new product schema", categories.size());
        return categories;
    }
}
