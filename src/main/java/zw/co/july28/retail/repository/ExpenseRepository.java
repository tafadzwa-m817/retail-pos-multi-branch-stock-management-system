package zw.co.july28.retail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByBranchId(Long branchId);
    List<Expense> findByBranchIdAndExpenseDateBetween(Long branchId, LocalDate start, LocalDate end);
    List<Expense> findByExpenseDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.branch.id = :branchId AND e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumByBranchAndDateRange(@Param("branchId") Long branchId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
