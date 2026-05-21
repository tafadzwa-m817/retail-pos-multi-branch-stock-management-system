package zw.co.july28.retail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.ProductReturn;

import java.util.List;

@Repository
public interface ProductReturnRepository extends JpaRepository<ProductReturn, Long> {
    List<ProductReturn> findByOriginalSaleId(Long saleId);
    List<ProductReturn> findByBranchId(Long branchId);
    List<ProductReturn> findByProcessedById(Long userId);
}
