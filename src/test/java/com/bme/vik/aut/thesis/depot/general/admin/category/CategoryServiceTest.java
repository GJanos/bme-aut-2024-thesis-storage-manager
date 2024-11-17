package com.bme.vik.aut.thesis.depot.general.admin.category;

import com.bme.vik.aut.thesis.depot.exception.category.CategoryAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.category.CategoryNotFoundException;
import com.bme.vik.aut.thesis.depot.general.admin.category.dto.CreateCategoryRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldReturnEmptyCategoryListWhenNoCategoryExist() {
        //***** <-- given: No categories in repository --> *****//
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        //***** <-- when: Retrieve all categories --> *****//
        List<Category> categories = categoryService.getAllCategories();

        //***** <-- then: Expect empty list --> *****//
        assertTrue(categories.isEmpty(), "Category list should be empty when no categories exist");
        verify(categoryRepository).findAll();
    }

    @Test
    void shouldReturnAllCategoriesWhenMultipleCategoriesExist() {
        //***** <-- given: Multiple categories in repository --> *****//
        List<Category> mockCategories = List.of(
                Category.builder().id(1L).name("Category1").description("Description1").build(),
                Category.builder().id(2L).name("Category2").description("Description2").build()
        );
        when(categoryRepository.findAll()).thenReturn(mockCategories);

        //***** <-- when: Retrieve all categories --> *****//
        List<Category> categories = categoryService.getAllCategories();

        //***** <-- then: Expect list of categories --> *****//
        assertEquals(2, categories.size(), "Category list should contain two categories");
        verify(categoryRepository).findAll();
    }

    @Test
    void shouldReturnCategoryByIdWhenValidIdProvided() {
        //***** <-- given: Category with valid ID --> *****//
        Long categoryId = 1L;
        Category mockCategory = Category.builder().id(categoryId).name("Category1").description("Description1").build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(mockCategory));

        //***** <-- when: Retrieve category by ID --> *****//
        Category category = categoryService.getCategoryById(categoryId);

        //***** <-- then: Verify returned category --> *****//
        assertNotNull(category);
        assertEquals("Category1", category.getName());
        assertEquals("Description1", category.getDescription());
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void shouldThrowExceptionWhenRequestingCategoryAndInvalidIdProvided() {
        //***** <-- given: Invalid category ID --> *****//
        Long invalidCategoryId = -1L;
        when(categoryRepository.findById(invalidCategoryId)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to retrieve category by invalid ID --> *****//
        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(invalidCategoryId));
        verify(categoryRepository).findById(invalidCategoryId);
    }

    @Test
    void shouldCreateCategoryWhenValidDetailsProvided() {
        //***** <-- given: Valid category details --> *****//
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Category")
                .description("Description")
                .build();

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        when(categoryRepository.existsByName(request.getName())).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        //***** <-- when: Create category --> *****//
        Category createdCategory = categoryService.createCategory(request);

        //***** <-- then: Verify created category --> *****//
        assertNotNull(createdCategory);
        assertEquals("Category", createdCategory.getName());
        assertEquals("Description", createdCategory.getDescription());
        verify(categoryRepository).existsByName(request.getName());
        verify(categoryRepository).save(category);
    }

    @Test
    void shouldNotCreateCategoryWhenDuplicateNameProvided() {
        //***** <-- given: Duplicate category name --> *****//
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("DuplicateCategory")
                .description("Description")
                .build();

        when(categoryRepository.existsByName(request.getName())).thenReturn(true);

        //***** <-- when & then: Attempt to create category with duplicate name --> *****//
        assertThrows(CategoryAlreadyExistsException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository).existsByName(request.getName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldUpdateCategorySuccessfullyWhenValidIdProvided() {
        //***** <-- given: Existing category with valid ID --> *****//
        Long categoryId = 1L;
        CreateCategoryRequest updateRequest = CreateCategoryRequest.builder()
                .name("UpdatedCategory")
                .description("Updated Description")
                .build();

        Category existingCategory = Category.builder()
                .id(categoryId)
                .name("OldCategory")
                .description("Old Description")
                .build();

        Category updatedCategory = Category.builder()
                .id(categoryId)
                .name(updateRequest.getName())
                .description(updateRequest.getDescription())
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByName(updateRequest.getName())).thenReturn(false);
        when(categoryRepository.save(updatedCategory)).thenReturn(updatedCategory);

        //***** <-- when: Update category --> *****//
        Category result = categoryService.updateCategory(categoryId, updateRequest);

        //***** <-- then: Verify update --> *****//
        assertNotNull(result);
        assertEquals(updateRequest.getName(), result.getName());
        assertEquals(updateRequest.getDescription(), result.getDescription());
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).existsByName(updateRequest.getName());
        verify(categoryRepository).save(updatedCategory);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingCategoryWithInvalidId() {
        //***** <-- given: Non-existent category ID --> *****//
        Long invalidCategoryId = 999L;
        CreateCategoryRequest updateRequest = CreateCategoryRequest.builder()
                .name("UpdatedCategory")
                .description("Updated Description")
                .build();

        when(categoryRepository.findById(invalidCategoryId)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to update with invalid ID --> *****//
        assertThrows(CategoryNotFoundException.class, () -> categoryService.updateCategory(invalidCategoryId, updateRequest));
        verify(categoryRepository).findById(invalidCategoryId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingCategoryWithDuplicateName() {
        //***** <-- given: Duplicate category name in another category --> *****//
        Long categoryId = 1L;
        CreateCategoryRequest updateRequest = CreateCategoryRequest.builder()
                .name("DuplicateCategory")
                .description("Updated Description")
                .build();

        Category existingCategory = Category.builder()
                .id(categoryId)
                .name("OldCategory")
                .description("Old Description")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByName(updateRequest.getName())).thenReturn(true);

        //***** <-- when & then: Attempt to update with duplicate name --> *****//
        assertThrows(CategoryAlreadyExistsException.class, () -> categoryService.updateCategory(categoryId, updateRequest));
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldDeleteCategoryByIdWhenValidIdProvided() {
        //***** <-- given: Existing category ID --> *****//
        Long categoryId = 1L;
        Category existingCategory = Category.builder().id(categoryId).name("Category").description("Description").build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        doNothing().when(categoryRepository).delete(existingCategory);

        //***** <-- when: Delete category by ID --> *****//
        categoryService.deleteCategory(categoryId);

        //***** <-- then: Verify deletion --> *****//
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).delete(existingCategory);
    }

    @Test
    void shouldThrowExceptionWhenDeletingCategoryWithInvalidId() {
        //***** <-- given: Non-existent category ID --> *****//
        Long invalidCategoryId = 999L;

        when(categoryRepository.findById(invalidCategoryId)).thenReturn(Optional.empty());

        //***** <-- when & then: Attempt to delete with invalid ID --> *****//
        assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategory(invalidCategoryId));
        verify(categoryRepository).findById(invalidCategoryId);
        verify(categoryRepository, never()).delete(any());
    }
}
