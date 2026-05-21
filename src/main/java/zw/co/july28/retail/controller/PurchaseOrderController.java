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
import zw.co.july28.retail.dto.request.PurchaseOrderRequest;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.dto.response.PurchaseOrderResponse;
import zw.co.july28.retail.service.PurchaseOrderService;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders", description = "Supplier purchase order management")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseOrderController {

    private final PurchaseOrderService orderService;

    @GetMapping
    @Operation(summary = "Get all purchase orders")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order by ID")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrder(id)));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get orders by branch")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersByBranch(branchId)));
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Get orders by supplier")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getBySupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersBySupplier(supplierId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Create a draft purchase order")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createOrder(
            @Valid @RequestBody PurchaseOrderRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Purchase order created", orderService.createOrder(request, user.getUsername())));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Submit a draft order to supplier")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> submitOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Order submitted", orderService.submitOrder(id)));
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Receive goods — adds stock to branch inventory")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receiveOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Order received and stock updated", orderService.receiveOrder(id)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Cancel a purchase order")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled", orderService.cancelOrder(id)));
    }
}
