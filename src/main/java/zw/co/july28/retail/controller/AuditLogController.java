package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.dto.response.AuditLogResponse;
import zw.co.july28.retail.dto.response.PageResponse;
import zw.co.july28.retail.service.AuditLogService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Audit Logs", description = "System audit trail — who did what and when")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Get all audit logs (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getAll(pageable)));
    }

    @GetMapping("/entity/{type}")
    @Operation(summary = "Filter audit logs by entity type (e.g. Sale, User, Branch)")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getByEntityType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getByEntityType(type, pageable)));
    }

    @GetMapping("/user/{email}")
    @Operation(summary = "Get audit logs for a specific user")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getByUser(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getByUser(email, pageable)));
    }

    @GetMapping("/entity/{type}/{id}")
    @Operation(summary = "Get audit history for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByEntity(
            @PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getByEntity(type, id)));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get audit logs within a date range")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getByDateRange(start, end, pageable)));
    }
}
