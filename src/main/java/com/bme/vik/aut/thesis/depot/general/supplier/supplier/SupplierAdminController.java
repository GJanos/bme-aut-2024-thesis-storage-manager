package com.bme.vik.aut.thesis.depot.general.supplier.supplier;

import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.CreateSupplierRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto.SupplierCreationResponse;
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
@RequestMapping("/admin/supplier")
@RequiredArgsConstructor
@Tag(name = "Supplier Management", description = "Administrative operations for suppliers.")
@PreAuthorize("hasRole('ADMIN')")
public class SupplierAdminController {

    private final SupplierService supplierService;

    @Operation(
            summary = "Get all suppliers",
            description = "Fetches all suppliers in the system.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Suppliers fetched successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Supplier.class))
                    )
            }
    )
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin:read')")
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }

    @Operation(
            summary = "Get supplier by ID",
            description = "Fetches a supplier by its ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Supplier found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Supplier.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Supplier not found",
                            content = @Content
                    )
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:read')")
    public ResponseEntity<Supplier> getSupplierById(
            @Parameter(description = "ID of the supplier to be fetched", required = true)
            @PathVariable Long id) {
        Supplier supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(supplier);
    }

    @Operation(
            summary = "Create a new supplier",
            description = "Creates a new supplier with an associated inventory.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Supplier created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Supplier.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Supplier already exists",
                            content = @Content
                    )
            }
    )
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin:create')")
    public ResponseEntity<SupplierCreationResponse> createSupplier(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details for creating a new supplier",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateSupplierRequest.class),
                            examples = @ExampleObject(
                                    value = "{ \"name\": \"supplier\", \"email\": \"supplier@example.com\", \"lowStockAlertThreshold\": 5, \"expiryAlertThreshold\": 3, \"reorderThreshold\": 2, \"reorderQuantity\": 1 }"
                            )
                    )
            )
            @RequestBody CreateSupplierRequest request) {
        SupplierCreationResponse response = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(
            summary = "Update supplier by ID",
            description = "Updates an existing supplier with the provided ID and details.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Supplier updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Supplier.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Supplier not found",
                            content = @Content
                    )
            }
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:update')")
    public ResponseEntity<Supplier> updateSupplier(
            @Parameter(description = "ID of the supplier to be updated", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details for updating the supplier",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateSupplierRequest.class),
                            examples = @ExampleObject(
                                    value = "{ \"name\": \"XYZ Supplies Updated\", \"email\": \"updated-supplier@example.com\", \"lowStockAlertThreshold\": 40, \"expiryAlertThreshold\": 20, \"reorderThreshold\": 15, \"reorderQuantity\": 50 }"
                            )
                    )
            )
            @RequestBody CreateSupplierRequest request) {
        Supplier updatedSupplier = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(updatedSupplier);
    }

    @Operation(
            summary = "Delete supplier by ID",
            description = "Deletes an existing supplier by its ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Supplier deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Supplier not found",
                            content = @Content
                    )
            }
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:delete')")
    public ResponseEntity<Void> deleteSupplier(
            @Parameter(description = "ID of the supplier to be deleted", required = true)
            @PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
