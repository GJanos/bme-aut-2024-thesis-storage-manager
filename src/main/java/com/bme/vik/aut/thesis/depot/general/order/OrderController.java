package com.bme.vik.aut.thesis.depot.general.order;

import com.bme.vik.aut.thesis.depot.general.order.dto.CreateOrderWithProductIdRequest;
import com.bme.vik.aut.thesis.depot.general.order.dto.CreateOrderWithProductSupplierRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
@RequestMapping("/order")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Operations related to creating and managing orders")
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Get all orders (Admin only)",
            description = "Fetches all orders. Admin access required.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Orders fetched successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order[].class))
                    )
            }
    )
    @GetMapping
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @Operation(
            summary = "Get order by ID (Admin only)",
            description = "Fetches an order by its ID. Admin access required.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Order fetched successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))
                    )
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }


    @Operation(
            summary = "Create a new order",
            description = "Creates a new order for the authenticated user for the given product IDs and quantities",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Order created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation errors or insufficient stock",
                            content = @Content
                    )
            }
    )
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<Order> createOrderByProductId(
            @RequestBody List<CreateOrderWithProductIdRequest> orderItems,
            @AuthenticationPrincipal MyUser userPrincipal) {
        Order order = orderService.createOrderByProductId(userPrincipal, orderItems);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(
            summary = "Create a new order",
            description = "Creates a new order for the authenticated user for the given product and supplier name pairs and quantities",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Order created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation errors or insufficient stock",
                            content = @Content
                    )
            }
    )
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<Order> createOrderByProductAndSupplierName(
            @RequestBody List<CreateOrderWithProductSupplierRequest> orderItems,
            @AuthenticationPrincipal MyUser userPrincipal) {
        Order order = orderService.createOrderByProductAndSupplierName(userPrincipal, orderItems);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(
            summary = "Cancel an order",
            description = "Cancels a user's order if it is in PENDING state",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Order cancelled successfully"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "User not authorized to cancel the order",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Order cannot be cancelled",
                            content = @Content
                    )
            }
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id, @AuthenticationPrincipal MyUser userPrincipal) {
        // TODO admin should be able to cancel any order
        orderService.cancelOrder(id, userPrincipal);
        return ResponseEntity.noContent().build();
    }
}
