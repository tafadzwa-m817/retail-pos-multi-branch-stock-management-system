package zw.co.july28.retail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.TaxRate;

import java.util.List;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    List<TaxRate> findByActiveTrue();

    @Query("SELECT t FROM TaxRate t WHERE t.active = true AND (t.branch IS NULL OR t.branch.id = :branchId)")
    List<TaxRate> findApplicableForBranch(@Param("branchId") Long branchId);
}
