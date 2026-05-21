package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.entity.SupplierProduct;
import zw.co.july28.retail.service.SupplierProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supplier-products")
@RequiredArgsConstructor
@Tag(name = "Supplier Catalog", description = "Supplier price catalog management")
@SecurityRequirement(name = "bearerAuth")
public class SupplierProductController {

    private final SupplierProductService supplierProductService;

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Get price catalog for a supplier")
    public ResponseEntity<ApiResponse<List<SupplierProduct>>> getCatalog(@PathVariable Long supplierId) {
        return ResponseEntity.ok(ApiResponse.ok(supplierProductService.getCatalogForSupplier(supplierId)));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get all suppliers for a product with their pricing")
    public ResponseEntity<ApiResponse<List<SupplierProduct>>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(supplierProductService.getSuppliersForProduct(productId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Add or update a product in a supplier's catalog")
    public ResponseEntity<ApiResponse<SupplierProduct>> upsert(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok("Catalog updated",
                supplierProductService.upsert(
                        Long.valueOf(body.get("supplierId").toString()),
                        Long.valueOf(body.get("productId").toString()),
                        new BigDecimal(body.get("unitCost").toString()),
                        body.containsKey("supplierSku") ? body.get("supplierSku").toString() : null,
                        Boolean.parseBoolean(body.getOrDefault("preferred", "false").toString())
                )));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Remove a product from a supplier's catalog")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        supplierProductService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Removed from catalog", null));
    }
}
