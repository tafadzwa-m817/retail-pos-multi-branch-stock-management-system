package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.PurchaseOrder;
import zw.co.july28.retail.entity.PurchaseOrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long branchId;
    private String branchName;
    private String status;
    private Long orderedById;
    private String orderedByName;
    private BigDecimal totalAmount;
    private String notes;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime receivedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal unitCost;
        private BigDecimal totalCost;

        public static OrderItemResponse from(PurchaseOrderItem item) {
            return OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .quantity(item.getQuantity())
                    .unitCost(item.getUnitCost())
                    .totalCost(item.getTotalCost())
                    .build();
        }
    }

    public static PurchaseOrderResponse from(PurchaseOrder order) {
        return PurchaseOrderResponse.builder()
                .id(order.getId())
                .supplierId(order.getSupplier().getId())
                .supplierName(order.getSupplier().getName())
                .branchId(order.getBranch().getId())
                .branchName(order.getBranch().getName())
                .status(order.getStatus().name())
                .orderedById(order.getOrderedBy() != null ? order.getOrderedBy().getId() : null)
                .orderedByName(order.getOrderedBy() != null
                        ? order.getOrderedBy().getFirstName() + " " + order.getOrderedBy().getLastName() : null)
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .items(order.getItems().stream().map(OrderItemResponse::from).collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .receivedAt(order.getReceivedAt())
                .build();
    }
}
