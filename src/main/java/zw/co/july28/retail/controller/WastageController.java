package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.entity.Wastage;
import zw.co.july28.retail.enums.WastageReason;
import zw.co.july28.retail.service.WastageService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wastage")
@RequiredArgsConstructor
@Tag(name = "Wastage", description = "Stock write-off tracking for damaged, expired or stolen items")
@SecurityRequirement(name = "bearerAuth")
public class WastageController {

    private final WastageService wastageService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Get all wastage records")
    public ResponseEntity<ApiResponse<List<Wastage>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(wastageService.getAll()));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get wastage records for a branch")
    public ResponseEntity<ApiResponse<List<Wastage>>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(wastageService.getByBranch(branchId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Record a stock write-off — deducts from inventory")
    public ResponseEntity<ApiResponse<Wastage>> record(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        var req = new WastageService.WastageRequest(
                Long.valueOf(body.get("branchId").toString()),
                Long.valueOf(body.get("productId").toString()),
                Integer.parseInt(body.get("quantity").toString()),
                WastageReason.valueOf(body.get("reason").toString()),
                body.containsKey("notes") ? body.get("notes").toString() : null,
                body.containsKey("wastedAt") ? LocalDate.parse(body.get("wastedAt").toString()) : null
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Wastage recorded", wastageService.recordWastage(req, user.getUsername())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete a wastage record")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        wastageService.deleteWastage(id);
        return ResponseEntity.ok(ApiResponse.ok("Wastage deleted", null));
    }
}
