package com.bme.vik.aut.thesis.depot.general.report;

import com.bme.vik.aut.thesis.depot.general.report.dto.InventoryStateReportResponse;
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
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/state")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<InventoryStateReportResponse> getInventoryStateReport() {
        InventoryStateReportResponse report = reportService.getInventoryStateReportResponse();
        return ResponseEntity.ok(report);
    }
}