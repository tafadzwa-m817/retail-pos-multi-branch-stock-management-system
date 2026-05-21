package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.request.CloseShiftRequest;
import zw.co.july28.retail.dto.request.OpenShiftRequest;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.dto.response.CashShiftResponse;
import zw.co.july28.retail.service.CashShiftService;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@Tag(name = "Cash Shifts", description = "Cashier shift management with float tracking")
@SecurityRequirement(name = "bearerAuth")
public class CashShiftController {

    private final CashShiftService cashShiftService;

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get shift history for a branch")
    public ResponseEntity<ApiResponse<List<CashShiftResponse>>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(cashShiftService.getShiftsByBranch(branchId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shift by ID")
    public ResponseEntity<ApiResponse<CashShiftResponse>> getShift(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(cashShiftService.getShift(id)));
    }

    @GetMapping("/branch/{branchId}/current")
    @Operation(summary = "Get the currently open shift for a branch")
    public ResponseEntity<ApiResponse<CashShiftResponse>> getCurrentShift(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(cashShiftService.getCurrentShift(branchId)));
    }

    @PostMapping("/open")
    @Operation(summary = "Open a new shift with opening float")
    public ResponseEntity<ApiResponse<CashShiftResponse>> openShift(
            @Valid @RequestBody OpenShiftRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Shift opened", cashShiftService.openShift(request, user.getUsername())));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "Close a shift — calculates variance against sales")
    public ResponseEntity<ApiResponse<CashShiftResponse>> closeShift(
            @PathVariable Long id,
            @Valid @RequestBody CloseShiftRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok("Shift closed", cashShiftService.closeShift(id, request, user.getUsername())));
    }
}
