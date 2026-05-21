package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.Sale;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleResponse {
    private Long id;
    private Long branchId;
    private String branchName;
    private Long cashierId;
    private String cashierName;
    private Long customerId;
    private String customerName;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentReference;
    private String status;
    private String notes;
    private List<SaleItemResponse> items;
    private LocalDateTime createdAt;

    public static SaleResponse from(Sale sale) {
        return SaleResponse.builder()
                .id(sale.getId())
                .branchId(sale.getBranch().getId())
                .branchName(sale.getBranch().getName())
                .cashierId(sale.getCashier().getId())
                .cashierName(sale.getCashier().getFirstName() + " " + sale.getCashier().getLastName())
                .customerId(sale.getCustomer() != null ? sale.getCustomer().getId() : null)
                .customerName(sale.getCustomer() != null
                        ? sale.getCustomer().getFirstName() + " " + sale.getCustomer().getLastName() : null)
                .subtotal(sale.getSubtotal())
                .discountAmount(sale.getDiscountAmount())
                .taxAmount(sale.getTaxAmount())
                .totalAmount(sale.getTotalAmount())
                .paymentMethod(sale.getPaymentMethod().name())
                .paymentReference(sale.getPaymentReference())
                .status(sale.getStatus().name())
                .notes(sale.getNotes())
                .items(sale.getItems().stream().map(SaleItemResponse::from).collect(Collectors.toList()))
                .createdAt(sale.getCreatedAt())
                .build();
    }
}
