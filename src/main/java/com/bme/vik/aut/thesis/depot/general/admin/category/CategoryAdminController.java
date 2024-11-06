package com.bme.vik.aut.thesis.depot.general.admin.category;

import com.bme.vik.aut.thesis.depot.general.admin.category.dto.CreateCategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "Administrative operations for categories.")
@PreAuthorize("hasRole('ADMIN')")
public class CategoryAdminController {

    private final CategoryService categoryService;

    @Operation(
            summary = "Get all categories",
            description = "Fetches all categories in the system.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Categories fetched successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))
                    )
            }
    )
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin:read')")
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(
            summary = "Get category by ID",
            description = "Fetches a category by its ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Category found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Category not found",
                            content = @Content
                    )
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:read')")
    public ResponseEntity<Category> getCategoryById(
            @Parameter(description = "ID of the category to be fetched", required = true)
            @PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @Operation(
            summary = "Create a new category",
            description = "Creates a new category with the provided details.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Category created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Category already exists",
                            content = @Content
                    )
            }
    )
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin:create')")
    public ResponseEntity<Category> createCategory(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details for creating a new category",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateCategoryRequest.class),
                            examples = @ExampleObject(value = "{ \"name\": \"Electronics\", \"description\": \"Category for electronic products\" }")
                    )
            )
            @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @Operation(
            summary = "Update category by ID",
            description = "Updates an existing category with the provided ID and details.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Category updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Category not found",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Category with the same name already exists",
                            content = @Content
                    )
            }
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:update')")
    public ResponseEntity<Category> updateCategory(
            @Parameter(description = "ID of the category to be updated", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details for updating the category",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateCategoryRequest.class),
                            examples = @ExampleObject(value = "{ \"name\": \"Updated Electronics\", \"description\": \"Updated description for electronics\" }")
                    )
            )
            @RequestBody CreateCategoryRequest request) {
        Category updatedCategory = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(updatedCategory);
    }

    @Operation(
            summary = "Delete category by ID",
            description = "Deletes an existing category by its ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Category deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Category not found",
                            content = @Content
                    )
            }
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:delete')")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "ID of the category to be deleted", required = true)
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}