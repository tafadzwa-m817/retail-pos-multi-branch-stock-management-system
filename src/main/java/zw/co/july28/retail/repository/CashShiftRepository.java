package zw.co.july28.retail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.CashShift;
import zw.co.july28.retail.enums.ShiftStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashShiftRepository extends JpaRepository<CashShift, Long> {
    Optional<CashShift> findByBranchIdAndStatus(Long branchId, ShiftStatus status);
    List<CashShift> findByBranchIdOrderByOpenedAtDesc(Long branchId);
    List<CashShift> findByOpenedById(Long userId);
    boolean existsByBranchIdAndStatus(Long branchId, ShiftStatus status);
}
