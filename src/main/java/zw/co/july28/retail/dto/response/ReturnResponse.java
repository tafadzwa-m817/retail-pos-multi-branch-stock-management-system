package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.ProductReturn;
import zw.co.july28.retail.entity.ReturnItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnResponse {
    private Long id;
    private Long originalSaleId;
    private Long branchId;
    private String branchName;
    private Long processedById;
    private String processedByName;
    private String reason;
    private BigDecimal totalRefundAmount;
    private List<ReturnItemDetail> items;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnItemDetail {
        private Long id;
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal unitRefundAmount;
        private BigDecimal totalRefundAmount;

        public static ReturnItemDetail from(ReturnItem item) {
            return ReturnItemDetail.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .quantity(item.getQuantity())
                    .unitRefundAmount(item.getUnitRefundAmount())
                    .totalRefundAmount(item.getTotalRefundAmount())
                    .build();
        }
    }

    public static ReturnResponse from(ProductReturn productReturn) {
        return ReturnResponse.builder()
                .id(productReturn.getId())
                .originalSaleId(productReturn.getOriginalSale().getId())
                .branchId(productReturn.getBranch().getId())
                .branchName(productReturn.getBranch().getName())
                .processedById(productReturn.getProcessedBy().getId())
                .processedByName(productReturn.getProcessedBy().getFirstName() + " " + productReturn.getProcessedBy().getLastName())
                .reason(productReturn.getReason())
                .totalRefundAmount(productReturn.getTotalRefundAmount())
                .items(productReturn.getItems().stream().map(ReturnItemDetail::from).collect(Collectors.toList()))
                .createdAt(productReturn.getCreatedAt())
                .build();
    }
}
