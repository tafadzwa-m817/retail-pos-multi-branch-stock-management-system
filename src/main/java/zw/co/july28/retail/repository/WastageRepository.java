package zw.co.july28.retail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.Wastage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WastageRepository extends JpaRepository<Wastage, Long> {
    List<Wastage> findByBranchId(Long branchId);
    List<Wastage> findByBranchIdAndWastedAtBetween(Long branchId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(w.quantity * p.costPrice), 0) FROM Wastage w JOIN w.product p WHERE w.branch.id = :branchId AND w.wastedAt BETWEEN :start AND :end")
    BigDecimal sumWastageValueByBranchAndDateRange(@Param("branchId") Long branchId,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);
}
