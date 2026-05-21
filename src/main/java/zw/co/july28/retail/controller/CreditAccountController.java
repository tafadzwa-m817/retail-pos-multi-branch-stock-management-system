package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.entity.CreditAccount;
import zw.co.july28.retail.entity.CreditTransaction;
import zw.co.july28.retail.service.CreditAccountService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credit-accounts")
@RequiredArgsConstructor
@Tag(name = "Credit Accounts", description = "Customer buy-on-credit account management")
@SecurityRequirement(name = "bearerAuth")
public class CreditAccountController {

    private final CreditAccountService creditAccountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Get all credit accounts")
    public ResponseEntity<ApiResponse<List<CreditAccount>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(creditAccountService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get credit account by ID")
    public ResponseEntity<ApiResponse<CreditAccount>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(creditAccountService.getById(id)));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get credit account for a customer")
    public ResponseEntity<ApiResponse<CreditAccount>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(creditAccountService.getByCustomer(customerId)));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Get transaction history for a credit account")
    public ResponseEntity<ApiResponse<List<CreditTransaction>>> getTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(creditAccountService.getTransactions(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Open a credit account for a customer")
    public ResponseEntity<ApiResponse<CreditAccount>> openAccount(@RequestBody Map<String, Object> body) {
        Long customerId     = Long.valueOf(body.get("customerId").toString());
        BigDecimal limit    = new BigDecimal(body.get("creditLimit").toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Credit account opened", creditAccountService.openAccount(customerId, limit)));
    }

    @PutMapping("/{id}/limit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update a credit account's limit")
    public ResponseEntity<ApiResponse<CreditAccount>> updateLimit(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal newLimit = new BigDecimal(body.get("creditLimit").toString());
        return ResponseEntity.ok(ApiResponse.ok("Limit updated", creditAccountService.updateLimit(id, newLimit)));
    }

    @PostMapping("/{id}/repayment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Record a repayment — reduces outstanding balance")
    public ResponseEntity<ApiResponse<CreditTransaction>> repayment(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String notes      = body.containsKey("notes") ? body.get("notes").toString() : null;
        return ResponseEntity.ok(ApiResponse.ok("Repayment recorded", creditAccountService.recordRepayment(id, amount, notes)));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Close a credit account (only if balance is zero)")
    public ResponseEntity<ApiResponse<Void>> close(@PathVariable Long id) {
        creditAccountService.closeAccount(id);
        return ResponseEntity.ok(ApiResponse.ok("Account closed", null));
    }
}
