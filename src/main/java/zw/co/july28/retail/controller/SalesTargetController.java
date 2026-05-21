package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.entity.SalesTarget;
import zw.co.july28.retail.service.SalesTargetService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-targets")
@RequiredArgsConstructor
@Tag(name = "Sales Targets", description = "Monthly revenue target management per branch")
@SecurityRequirement(name = "bearerAuth")
public class SalesTargetController {

    private final SalesTargetService targetService;

    @GetMapping
    @Operation(summary = "Get all sales targets")
    public ResponseEntity<ApiResponse<List<SalesTarget>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(targetService.getAllTargets()));
    }

    @GetMapping("/progress")
    @Operation(summary = "Get current month targets with % achieved per branch")
    public ResponseEntity<ApiResponse<List<SalesTargetService.TargetProgress>>> getProgress() {
        return ResponseEntity.ok(ApiResponse.ok(targetService.getCurrentMonthProgress()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Set or update a monthly revenue target for a branch")
    public ResponseEntity<ApiResponse<SalesTarget>> setTarget(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        Long branchId    = Long.valueOf(body.get("branchId").toString());
        BigDecimal amount = new BigDecimal(body.get("targetAmount").toString());
        int month        = Integer.parseInt(body.get("month").toString());
        int year         = Integer.parseInt(body.get("year").toString());
        return ResponseEntity.ok(ApiResponse.ok("Target set", targetService.setTarget(branchId, amount, month, year, user.getUsername())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete a sales target")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        targetService.deleteTarget(id);
        return ResponseEntity.ok(ApiResponse.ok("Target deleted", null));
    }
}
