package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.entity.TaxRate;
import zw.co.july28.retail.service.TaxRateService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/tax-rates")
@RequiredArgsConstructor
@Tag(name = "Tax Rates", description = "Tax rate configuration")
@SecurityRequirement(name = "bearerAuth")
public class TaxRateController {

    private final TaxRateService taxRateService;

    @GetMapping
    @Operation(summary = "Get all tax rates")
    public ResponseEntity<ApiResponse<List<TaxRate>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(taxRateService.getAll()));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get applicable tax rates for a branch")
    public ResponseEntity<ApiResponse<List<TaxRate>>> getForBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(taxRateService.getForBranch(branchId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create tax rate")
    public ResponseEntity<ApiResponse<TaxRate>> create(
            @RequestParam String name,
            @RequestParam BigDecimal rate,
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok("Tax rate created", taxRateService.create(name, rate, branchId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update tax rate")
    public ResponseEntity<ApiResponse<TaxRate>> update(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam BigDecimal rate,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.ok("Tax rate updated", taxRateService.update(id, name, rate, active)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete tax rate")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        taxRateService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Tax rate deleted", null));
    }
}
