package com.bme.vik.aut.thesis.depot.general.supplier.supplier;

import com.bme.vik.aut.thesis.depot.general.report.dto.InventoryState;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.InventoryService;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.CreateProductStockRequest;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.ProductStockResponse;
import com.bme.vik.aut.thesis.depot.general.supplier.product.dto.RemoveProductStockRequest;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/supplier")
@RequiredArgsConstructor
@Tag(name = "Supplier Inventory Stock Management", description = "Operations for managing inventory stock by suppliers.")
@PreAuthorize("hasRole('SUPPLIER')")
public class SupplierStockController {

    private final InventoryService inventoryService;

    @Operation(
            summary = "Get supplier's inventory",
            description = "Fetches the inventory details of the supplier.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Inventory fetched successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = InventoryState.class))
                    )
            }
    )
    @GetMapping("/inventory")
    @PreAuthorize("hasAuthority('supplier:read')")
    public ResponseEntity<InventoryState> getInventory(@AuthenticationPrincipal MyUser userPrincipal) {
        InventoryState inventoryState = inventoryService.getInventoryStateBySupplierId(userPrincipal.getSupplierId());
        return ResponseEntity.ok(inventoryState);
    }

    @Operation(
            summary = "Add stock to supplier's inventory",
            description = "Adds products to the supplier's inventory.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Stock added successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateProductStockRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Inventory full error",
                            content = @Content
                    )
            }
    )
    @PostMapping("/add-stock")
    @PreAuthorize("hasAuthority('supplier:create')")
    public ResponseEntity<ProductStockResponse> addStock(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details for adding stock to the supplier's inventory",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateProductStockRequest.class),
                            examples = @ExampleObject(
                                    value = "{ \"productSchemaId\": 1, \"description\": \"Sample product description\", \"quantity\": 10, \"expiresAt\": \"2024-12-07T19:29:10.378Z\" }"
                            )
                    )
            )
            @RequestBody CreateProductStockRequest productStock, @AuthenticationPrincipal MyUser userPrincipal) {
        ProductStockResponse response = inventoryService.addStock(userPrincipal, productStock);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Remove stock from supplier's inventory",
            description = "Removes products from the supplier's inventory.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Stock removed successfully"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Not enough products error",
                            content = @Content
                    )
            }
    )
    @PostMapping("/remove-stock")
    @PreAuthorize("hasAuthority('supplier:delete')")
    public ResponseEntity<ProductStockResponse> removeStock(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details for removing stock from the supplier's inventory",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RemoveProductStockRequest.class),
                            examples = @ExampleObject(
                                    value = "{ \"productSchemaId\": 1, \"quantity\": 5 }"
                            )
                    )
            )
            @RequestBody RemoveProductStockRequest productStock, @AuthenticationPrincipal MyUser userPrincipal) {
        ProductStockResponse response = inventoryService.removeStock(userPrincipal, productStock);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(
            summary = "Get all products in supplier's inventory",
            description = "Fetches all products stored in the supplier's inventory.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Products fetched successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))
                    )
            }
    )
    @GetMapping("/product")
    @PreAuthorize("hasAuthority('supplier:read')")
    public ResponseEntity<List<Product>> getAllProducts(@AuthenticationPrincipal MyUser userPrincipal) {
        List<Product> products = inventoryService.getAllProductsInInventoryForUser(userPrincipal);
        return ResponseEntity.ok(products);
    }
}
