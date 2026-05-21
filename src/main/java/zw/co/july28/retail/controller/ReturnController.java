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
import zw.co.july28.retail.dto.request.ReturnRequest;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.dto.response.ReturnResponse;
import zw.co.july28.retail.service.ReturnService;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
@Tag(name = "Product Returns", description = "Return and refund management")
@SecurityRequirement(name = "bearerAuth")
public class ReturnController {

    private final ReturnService returnService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Get all returns")
    public ResponseEntity<ApiResponse<List<ReturnResponse>>> getAllReturns() {
        return ResponseEntity.ok(ApiResponse.ok(returnService.getAllReturns()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get return by ID")
    public ResponseEntity<ApiResponse<ReturnResponse>> getReturn(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(returnService.getReturn(id)));
    }

    @GetMapping("/sale/{saleId}")
    @Operation(summary = "Get returns for a specific sale")
    public ResponseEntity<ApiResponse<List<ReturnResponse>>> getBySale(@PathVariable Long saleId) {
        return ResponseEntity.ok(ApiResponse.ok(returnService.getReturnsBySale(saleId)));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get returns by branch")
    public ResponseEntity<ApiResponse<List<ReturnResponse>>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(returnService.getReturnsByBranch(branchId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER', 'CASHIER')")
    @Operation(summary = "Process a product return — restores stock and records refund")
    public ResponseEntity<ApiResponse<ReturnResponse>> processReturn(
            @Valid @RequestBody ReturnRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Return processed", returnService.processReturn(request, user.getUsername())));
    }
}
