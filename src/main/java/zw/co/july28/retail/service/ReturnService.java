package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.july28.retail.dto.request.ReturnRequest;
import zw.co.july28.retail.dto.response.ReturnResponse;
import zw.co.july28.retail.entity.*;
import zw.co.july28.retail.enums.AuditAction;
import zw.co.july28.retail.enums.SaleStatus;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.AppUserRepository;
import zw.co.july28.retail.repository.ProductReturnRepository;
import zw.co.july28.retail.repository.SaleRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ProductReturnRepository returnRepository;
    private final SaleRepository saleRepository;
    private final AppUserRepository userRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    public List<ReturnResponse> getAllReturns() {
        return returnRepository.findAll().stream()
                .map(ReturnResponse::from)
                .collect(Collectors.toList());
    }

    public ReturnResponse getReturn(Long id) {
        return ReturnResponse.from(findById(id));
    }

    public List<ReturnResponse> getReturnsBySale(Long saleId) {
        return returnRepository.findByOriginalSaleId(saleId).stream()
                .map(ReturnResponse::from)
                .collect(Collectors.toList());
    }

    public List<ReturnResponse> getReturnsByBranch(Long branchId) {
        return returnRepository.findByBranchId(branchId).stream()
                .map(ReturnResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReturnResponse processReturn(ReturnRequest request, String userEmail) {
        Sale originalSale = saleRepository.findById(request.getOriginalSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", request.getOriginalSaleId()));

        if (originalSale.getStatus() == SaleStatus.VOIDED) {
            throw new BadRequestException("Cannot return items from a voided sale");
        }

        AppUser processor = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Map<Long, SaleItem> saleItemMap = originalSale.getItems().stream()
                .collect(Collectors.toMap(SaleItem::getId, Function.identity()));

        ProductReturn productReturn = ProductReturn.builder()
                .originalSale(originalSale)
                .branch(originalSale.getBranch())
                .processedBy(processor)
                .reason(request.getReason())
                .build();

        BigDecimal totalRefund = BigDecimal.ZERO;

        for (ReturnRequest.ReturnItemRequest itemReq : request.getItems()) {
            SaleItem saleItem = saleItemMap.get(itemReq.getSaleItemId());
            if (saleItem == null) {
                throw new BadRequestException("Sale item ID " + itemReq.getSaleItemId() + " not found in sale " + originalSale.getId());
            }

            int alreadyReturned = returnRepository.findByOriginalSaleId(originalSale.getId()).stream()
                    .flatMap(r -> r.getItems().stream())
                    .filter(ri -> ri.getOriginalSaleItem().getId().equals(itemReq.getSaleItemId()))
                    .mapToInt(ReturnItem::getQuantity)
                    .sum();

            int maxReturnable = saleItem.getQuantity() - alreadyReturned;
            if (itemReq.getQuantity() > maxReturnable) {
                throw new BadRequestException("Cannot return " + itemReq.getQuantity() + " of '" +
                        saleItem.getProduct().getName() + "' — only " + maxReturnable + " returnable");
            }

            BigDecimal unitRefund = saleItem.getUnitPrice().subtract(
                    saleItem.getDiscountAmount().divide(BigDecimal.valueOf(saleItem.getQuantity()), 2, java.math.RoundingMode.HALF_UP)
            );
            BigDecimal itemRefund = unitRefund.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalRefund = totalRefund.add(itemRefund);

            ReturnItem returnItem = ReturnItem.builder()
                    .productReturn(productReturn)
                    .originalSaleItem(saleItem)
                    .product(saleItem.getProduct())
                    .quantity(itemReq.getQuantity())
                    .unitRefundAmount(unitRefund)
                    .totalRefundAmount(itemRefund)
                    .build();

            productReturn.getItems().add(returnItem);

            inventoryService.addStock(saleItem.getProduct(), originalSale.getBranch(), itemReq.getQuantity());
        }

        productReturn.setTotalRefundAmount(totalRefund);
        ProductReturn saved = returnRepository.save(productReturn);

        originalSale.setStatus(SaleStatus.REFUNDED);
        saleRepository.save(originalSale);

        auditLogService.log("ProductReturn", saved.getId(), AuditAction.CREATE,
                "Return processed for sale #" + originalSale.getId() + " — refund: $" + totalRefund);

        return ReturnResponse.from(saved);
    }

    private ProductReturn findById(Long id) {
        return returnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Return", id));
    }
}
