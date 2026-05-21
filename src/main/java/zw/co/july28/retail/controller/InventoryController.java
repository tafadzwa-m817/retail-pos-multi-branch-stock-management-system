package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.request.InventoryAdjustRequest;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.dto.response.InventoryResponse;
import zw.co.july28.retail.service.InventoryService;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management per branch")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Get all inventory")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventory() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllInventory()));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get inventory for a specific branch")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getInventoryByBranch(branchId)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock items (optionally filtered by branch)")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStock(
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowStockItems(branchId)));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Manually adjust stock quantity")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @Valid @RequestBody InventoryAdjustRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted", inventoryService.adjustInventory(request)));
    }

    @GetMapping("/reorder-needed")
    @Operation(summary = "Get all items needing reorder — raw inventory records below reorder level")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getReorderNeeded(
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowStockItems(branchId)));
    }
}
