package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.request.SaleRequest;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.dto.response.PageResponse;
import zw.co.july28.retail.dto.response.SaleResponse;
import zw.co.july28.retail.service.SaleService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Tag(name = "Sales / POS", description = "Point of sale transactions")
@SecurityRequirement(name = "bearerAuth")
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Get all sales (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<SaleResponse>>> getAllSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(saleService.getAllSales(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sale by ID")
    public ResponseEntity<ApiResponse<SaleResponse>> getSale(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(saleService.getSale(id)));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get sales by branch")
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(saleService.getSalesByBranch(branchId)));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get sales by date range (optionally filtered by branch)")
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getByDateRange(
            @RequestParam(required = false) Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(ApiResponse.ok(saleService.getSalesByDateRange(branchId, start, end)));
    }

    @PostMapping
    @Operation(summary = "Process a new sale (POS transaction)")
    public ResponseEntity<ApiResponse<SaleResponse>> createSale(
            @Valid @RequestBody SaleRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sale processed successfully", saleService.createSale(request, user.getUsername())));
    }

    @PutMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Void a sale — restores inventory")
    public ResponseEntity<ApiResponse<SaleResponse>> voidSale(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Sale voided", saleService.voidSale(id)));
    }
}
