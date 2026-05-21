package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.entity.Branch;
import zw.co.july28.retail.entity.Expense;
import zw.co.july28.retail.entity.ExpenseCategory;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;

    public record ExpenseRequest(Long branchId, Long categoryId, String description, BigDecimal amount, LocalDate expenseDate, String notes) {}
    public record ExpenseCategoryRequest(String name, String color) {}

    public List<ExpenseCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public ExpenseCategory createCategory(ExpenseCategoryRequest req) {
        return categoryRepository.save(ExpenseCategory.builder().name(req.name()).color(req.color()).build());
    }

    public List<Expense> getAll() {
        return expenseRepository.findAll();
    }

    public List<Expense> getByBranch(Long branchId) {
        return expenseRepository.findByBranchId(branchId);
    }

    public List<Expense> getByDateRange(Long branchId, LocalDate start, LocalDate end) {
        return branchId != null
                ? expenseRepository.findByBranchIdAndExpenseDateBetween(branchId, start, end)
                : expenseRepository.findByExpenseDateBetween(start, end);
    }

    public BigDecimal getTotalExpenses(Long branchId, LocalDate start, LocalDate end) {
        return expenseRepository.sumByBranchAndDateRange(branchId, start, end);
    }

    public Expense createExpense(ExpenseRequest req, String userEmail) {
        Branch branch = branchRepository.findById(req.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", req.branchId()));
        ExpenseCategory category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", req.categoryId()));
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        return expenseRepository.save(Expense.builder()
                .branch(branch).category(category).description(req.description())
                .amount(req.amount()).expenseDate(req.expenseDate())
                .recordedBy(user).notes(req.notes()).build());
    }

    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }
}
