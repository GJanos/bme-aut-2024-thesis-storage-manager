package com.bme.vik.aut.thesis.depot.general.admin.productschema;

import com.bme.vik.aut.thesis.depot.general.admin.productschema.dto.CreateProductSchemaRequest;
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
@RequestMapping("/admin/product-schema")
@RequiredArgsConstructor
@Tag(name = "Product Schema Management", description = "Administrative operations for product schemas.")
@PreAuthorize("hasRole('ADMIN')")
public class ProductSchemaController {

    private final ProductSchemaService productSchemaService;

    @Operation(
            summary = "Get all product schemas",
            description = "Fetches all product schemas in the system.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Product schemas fetched successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductSchema.class))
                    )
            }
    )
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin:read')")
    public ResponseEntity<List<ProductSchema>> getAllProductSchemas() {
        List<ProductSchema> productSchemas = productSchemaService.getAllProductSchemas();
        return ResponseEntity.ok(productSchemas);
    }

    @Operation(
            summary = "Get product schema by ID",
            description = "Fetches a product schema by its ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Product schema found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductSchema.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Product schema not found",
                            content = @Content
                    )
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:read')")
    public ResponseEntity<ProductSchema> getProductSchemaById(
            @Parameter(description = "ID of the product schema to be fetched", required = true)
            @PathVariable Long id) {
        ProductSchema productSchema = productSchemaService.getProductSchemaById(id);
        return ResponseEntity.ok(productSchema);
    }

    @Operation(
            summary = "Create a new product schema",
            description = "Creates a new product schema with the provided details.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Product schema created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductSchema.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Product schema already exists",
                            content = @Content
                    )
            }
    )
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin:create')")
    public ResponseEntity<ProductSchema> createProductSchema(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details for creating a new product schema",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateProductSchemaRequest.class),
                            examples = @ExampleObject(value = "{ \"name\": \"Laptop\", \"storageSpaceNeeded\": 5.0, \"categoryIDs\": [1, 2] }")
                    )
            )
            @RequestBody CreateProductSchemaRequest request) {
        ProductSchema productSchema = productSchemaService.createProductSchema(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productSchema);
    }

    @Operation(
            summary = "Update product schema by ID",
            description = "Updates an existing product schema with the provided ID and details.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Product schema updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductSchema.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Product schema not found",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Product schema with the same name already exists",
                            content = @Content
                    )
            }
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:update')")
    public ResponseEntity<ProductSchema> updateProductSchema(
            @Parameter(description = "ID of the product schema to be updated", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details for updating the product schema",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateProductSchemaRequest.class),
                            examples = @ExampleObject(value = "{ \"name\": \"Updated Laptop\", \"storageSpaceNeeded\": 6.0, \"categoryIDs\": [1, 2] }")
                    )
            )
            @RequestBody CreateProductSchemaRequest request) {
        ProductSchema updatedProductSchema = productSchemaService.updateProductSchema(id, request);
        return ResponseEntity.ok(updatedProductSchema);
    }

    @Operation(
            summary = "Delete product schema by ID",
            description = "Deletes an existing product schema by its ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Product schema deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Product schema not found",
                            content = @Content
                    )
            }
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:delete')")
    public ResponseEntity<Void> deleteProductSchema(
            @Parameter(description = "ID of the product schema to be deleted", required = true)
            @PathVariable Long id) {
        productSchemaService.deleteProductSchema(id);
        return ResponseEntity.noContent().build();
    }
}


