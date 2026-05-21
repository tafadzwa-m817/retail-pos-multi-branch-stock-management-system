package zw.co.july28.retail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.Inventory;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductIdAndBranchId(Long productId, Long branchId);
    List<Inventory> findByBranchId(Long branchId);
    List<Inventory> findByProductId(Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.product.reorderLevel AND i.branch.id = :branchId")
    List<Inventory> findLowStockByBranch(@Param("branchId") Long branchId);

    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.product.reorderLevel")
    List<Inventory> findAllLowStock();
}
