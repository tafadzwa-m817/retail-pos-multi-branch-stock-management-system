package zw.co.july28.retail.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.Sale;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByBranchId(Long branchId);
    Page<Sale> findByBranchId(Long branchId, Pageable pageable);
    List<Sale> findByCashierId(Long cashierId);
    List<Sale> findByCustomerId(Long customerId);

    @Query("SELECT s FROM Sale s WHERE s.branch.id = :branchId AND s.createdAt BETWEEN :start AND :end")
    List<Sale> findByBranchAndDateRange(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT s FROM Sale s WHERE s.createdAt BETWEEN :start AND :end")
    List<Sale> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.branch.id = :branchId AND s.createdAt BETWEEN :start AND :end")
    BigDecimal sumRevenueByBranchAndDateRange(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
