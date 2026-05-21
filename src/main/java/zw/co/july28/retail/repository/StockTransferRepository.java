package zw.co.july28.retail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.StockTransfer;
import zw.co.july28.retail.enums.TransferStatus;

import java.util.List;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    List<StockTransfer> findByFromBranchIdOrToBranchId(Long fromBranchId, Long toBranchId);
    List<StockTransfer> findByStatus(TransferStatus status);
    List<StockTransfer> findByRequestedById(Long userId);
}
