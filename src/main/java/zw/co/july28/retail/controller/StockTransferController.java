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
import zw.co.july28.retail.dto.request.StockTransferRequest;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.dto.response.StockTransferResponse;
import zw.co.july28.retail.service.StockTransferService;

import java.util.List;

@RestController
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
@Tag(name = "Stock Transfers", description = "Inter-branch stock transfer management")
@SecurityRequirement(name = "bearerAuth")
public class StockTransferController {

    private final StockTransferService transferService;

    @GetMapping
    @Operation(summary = "Get all stock transfers")
    public ResponseEntity<ApiResponse<List<StockTransferResponse>>> getAllTransfers() {
        return ResponseEntity.ok(ApiResponse.ok(transferService.getAllTransfers()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock transfer by ID")
    public ResponseEntity<ApiResponse<StockTransferResponse>> getTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(transferService.getTransfer(id)));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get transfers involving a branch")
    public ResponseEntity<ApiResponse<List<StockTransferResponse>>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(transferService.getTransfersByBranch(branchId)));
    }

    @PostMapping
    @Operation(summary = "Request a new stock transfer")
    public ResponseEntity<ApiResponse<StockTransferResponse>> createTransfer(
            @Valid @RequestBody StockTransferRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transfer request created", transferService.createTransfer(request, user.getUsername())));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Approve a pending transfer")
    public ResponseEntity<ApiResponse<StockTransferResponse>> approveTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok("Transfer approved", transferService.approveTransfer(id, user.getUsername())));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Complete an approved transfer — deducts and adds stock")
    public ResponseEntity<ApiResponse<StockTransferResponse>> completeTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Transfer completed", transferService.completeTransfer(id)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Cancel a transfer")
    public ResponseEntity<ApiResponse<StockTransferResponse>> cancelTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Transfer cancelled", transferService.cancelTransfer(id)));
    }
}
