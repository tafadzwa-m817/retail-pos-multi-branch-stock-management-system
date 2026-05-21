package zw.co.july28.retail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.SalesTarget;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesTargetRepository extends JpaRepository<SalesTarget, Long> {
    List<SalesTarget> findByMonthAndYear(int month, int year);
    List<SalesTarget> findByBranchId(Long branchId);
    Optional<SalesTarget> findByBranchIdAndMonthAndYear(Long branchId, int month, int year);
}
