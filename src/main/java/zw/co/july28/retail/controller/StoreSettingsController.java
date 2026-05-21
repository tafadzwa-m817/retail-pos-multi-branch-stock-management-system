package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.request.StoreSettingsRequest;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.entity.StoreSettings;
import zw.co.july28.retail.service.StoreSettingsService;

@RestController
@RequestMapping("/api/store-settings")
@RequiredArgsConstructor
@Tag(name = "Store Settings", description = "Receipt customisation and store profile")
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    @GetMapping
    @Operation(summary = "Get current store settings (public — used by receipt)")
    public ResponseEntity<ApiResponse<StoreSettings>> get() {
        return ResponseEntity.ok(ApiResponse.ok(storeSettingsService.get()));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update store settings")
    public ResponseEntity<ApiResponse<StoreSettings>> update(@RequestBody StoreSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Settings updated", storeSettingsService.update(request)));
    }
}
