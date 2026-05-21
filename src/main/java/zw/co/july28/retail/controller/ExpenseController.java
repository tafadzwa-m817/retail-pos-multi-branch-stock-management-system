package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.entity.Expense;
import zw.co.july28.retail.entity.ExpenseCategory;
import zw.co.july28.retail.service.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Branch expense tracking")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping("/categories")
    @Operation(summary = "Get all expense categories")
    public ResponseEntity<ApiResponse<List<ExpenseCategory>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getAllCategories()));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create expense category")
    public ResponseEntity<ApiResponse<ExpenseCategory>> createCategory(@RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category created",
                        expenseService.createCategory(new ExpenseService.ExpenseCategoryRequest(body.get("name"), body.get("color")))));
    }

    @GetMapping
    @Operation(summary = "Get all expenses")
    public ResponseEntity<ApiResponse<List<Expense>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getAll()));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get expenses for a branch")
    public ResponseEntity<ApiResponse<List<Expense>>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getByBranch(branchId)));
    }

    @GetMapping("/range")
    @Operation(summary = "Get expenses by date range")
    public ResponseEntity<ApiResponse<List<Expense>>> getByRange(
            @RequestParam(required = false) Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getByDateRange(branchId, start, end)));
    }

    @GetMapping("/total")
    @Operation(summary = "Total expenses for a branch in a date range")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotal(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getTotalExpenses(branchId, start, end)));
    }

    @PostMapping
    @Operation(summary = "Record a new expense")
    public ResponseEntity<ApiResponse<Expense>> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        var req = new ExpenseService.ExpenseRequest(
                Long.valueOf(body.get("branchId").toString()),
                Long.valueOf(body.get("categoryId").toString()),
                body.get("description").toString(),
                new BigDecimal(body.get("amount").toString()),
                LocalDate.parse(body.get("expenseDate").toString()),
                body.containsKey("notes") ? body.get("notes").toString() : null
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Expense recorded", expenseService.createExpense(req, user.getUsername())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Delete expense")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.ok("Expense deleted", null));
    }
}
