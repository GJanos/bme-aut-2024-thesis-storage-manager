package com.bme.vik.aut.thesis.depot.general.order;

import com.bme.vik.aut.thesis.depot.general.order.dto.CreateOrderRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bme.vik.aut.thesis.depot.security.user.MyUser;
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
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order items with various ways to specify products",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "ByProductId",
                                            value = "[{\"type\": \"productId\", \"productId\": 72, \"quantity\": 1}]"
                                    ),
                                    @ExampleObject(
                                            name = "ByProductName",
                                            value = "[{\"type\": \"productName\", \"productName\": \"Laptop\", \"quantity\": 2}]"
                                    ),
                                    @ExampleObject(
                                            name = "ByProductAndSupplierName",
                                            value = "[{\"type\": \"productSupplierName\", \"supplierName\": \"TechSupplier\", \"productName\": \"Laptop\", \"quantity\": 5}]"
                                    )
                            }
                    )
            )
    )
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<Order> createOrder(
            @RequestBody List<CreateOrderRequest> orderItems,
            @AuthenticationPrincipal MyUser userPrincipal) {
        Order order = orderService.createOrder(userPrincipal, orderItems);
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
        orderService.cancelOrder(id, userPrincipal);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get all pending orders (Admin only)",
            description = "Fetches all orders that are currently in PENDING state. Admin access required.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Pending orders fetched successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order[].class))
                    )
            }
    )
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<List<Order>> getAllPendingOrders() {
        List<Order> pendingOrders = orderService.getAllPendingOrders();
        return ResponseEntity.ok(pendingOrders);
    }

    @Operation(
            summary = "Get a pending order by ID (Admin only)",
            description = "Fetches a pending order by its ID. Admin access required.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Pending order fetched successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Order not found or not in pending state",
                            content = @Content
                    )
            }
    )
    @GetMapping("/pending/{id}")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<Order> getPendingOrderById(@PathVariable Long id) {
        Order order = orderService.getPendingOrderById(id);
        return ResponseEntity.ok(order);
    }

    @Operation(
            summary = "Approve a pending order (Admin only)",
            description = "Sets the status of a pending order to approved, transitioning it out of the PENDING state. Admin access required.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Order approved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Order not found or not in pending state",
                            content = @Content
                    )
            }
    )
    @PutMapping("/pending/{id}/approve")
    @PreAuthorize("hasAuthority('admin:update')")
    public ResponseEntity<Order> approvePendingOrder(@PathVariable Long id) {
        Order approvedOrder = orderService.approvePendingOrder(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(approvedOrder);
    }
}
