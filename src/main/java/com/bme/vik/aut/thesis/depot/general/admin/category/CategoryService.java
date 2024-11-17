package com.bme.vik.aut.thesis.depot.general.admin.category;

import com.bme.vik.aut.thesis.depot.exception.category.CategoryAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.category.CategoryNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.category.dto.CreateCategoryRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        logger.info("Fetching all categories");
        List<Category> categories = categoryRepository.findAll();
        logger.info("Fetched {} categories", categories.size());
        return categories;
    }

    public Category getCategoryById(Long id) {
        logger.info("Fetching category with ID: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Category with ID {} not found", id);
                    return new CategoryNotFoundException("Category with ID " + id + " not found");
                });
    }

    public Category createCategory(CreateCategoryRequest request) {
        logger.info("Attempting to create new category with name: {}", request.getName());

        if (categoryRepository.existsByName(request.getName())) {
            logger.warn("Category creation failed: Category with name '{}' already exists", request.getName());
            throw new CategoryAlreadyExistsException("Category with name " + request.getName() + " already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category savedCategory = categoryRepository.save(category);
        logger.info("Category created successfully with ID: {}", savedCategory.getId());
        return savedCategory;
    }

    public Category updateCategory(Long id, CreateCategoryRequest request) {
        logger.info("Attempting to update category with ID: {}", id);

        Category category = getCategoryById(id);

        if (categoryRepository.existsByName(request.getName()) && !category.getName().equals(request.getName())) {
            logger.warn("Category update failed: Category with name '{}' already exists", request.getName());
            throw new CategoryAlreadyExistsException("Category with name " + request.getName() + " already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        Category updatedCategory = categoryRepository.save(category);
        logger.info("Category with ID: {} updated successfully", updatedCategory.getId());
        return updatedCategory;
    }

    public void deleteCategory(Long id) {
        logger.info("Attempting to delete category with ID: {}", id);

        Category category = getCategoryById(id);
        categoryRepository.delete(category);

        logger.info("Category with ID: {} deleted successfully", id);
    }
}
