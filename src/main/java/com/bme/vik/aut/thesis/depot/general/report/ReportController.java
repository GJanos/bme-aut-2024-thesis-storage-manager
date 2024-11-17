package com.bme.vik.aut.thesis.depot.general.report;

import com.bme.vik.aut.thesis.depot.general.report.dto.InventoryExpiryReportResponse;
import com.bme.vik.aut.thesis.depot.general.report.dto.InventoryStateReportResponse;
import com.bme.vik.aut.thesis.depot.general.report.dto.OrderReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Report Management", description = "Operations related to generating inventory and order reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "Get Inventory State Report",
            description = "Fetches a detailed report of the current inventory state, including maximum available space, used space, and breakdown by inventory.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Inventory state report generated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = InventoryStateReportResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden - Only accessible by users with admin privileges",
                            content = @Content
                    )
            }
    )
    @GetMapping("/state")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<InventoryStateReportResponse> getInventoryStateReport() {
        InventoryStateReportResponse report = reportService.getInventoryStateReportResponse();
        return ResponseEntity.ok(report);
    }

    @Operation(
            summary = "Get Inventory Expiry Report",
            description = "Generates a report detailing product expiry status across all inventories, including products that are close to expiry or already expired.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Inventory expiry report generated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = InventoryExpiryReportResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden - Only accessible by users with admin privileges",
                            content = @Content
                    )
            }
    )
    @GetMapping("/expiry")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<InventoryExpiryReportResponse> getInventoryExpiryReport() {
        InventoryExpiryReportResponse report = reportService.getInventoryExpiryReportResponse();
        return ResponseEntity.ok(report);
    }

    @Operation(
            summary = "Get Order Report",
            description = "Provides a report of orders, including the number of orders, order statuses, and details of each user's orders.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Order report generated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderReportResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden - Only accessible by users with admin privileges",
                            content = @Content
                    )
            }
    )
    @GetMapping("/order")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<OrderReportResponse> getOrderReport() {
        OrderReportResponse report = reportService.getOrderReportResponse();
        return ResponseEntity.ok(report);
    }
}