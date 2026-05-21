package zw.co.july28.retail.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import zw.co.july28.retail.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequest {

    @NotNull(message = "Branch is required")
    private Long branchId;

    private Long customerId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String paymentReference;

    @NotEmpty(message = "At least one item is required")
    private List<SaleItemRequest> items;

    private String notes;

    /** Loyalty points to redeem (1 point = $0.01 discount). Requires customerId to be set. */
    @Min(value = 0, message = "Loyalty points to redeem cannot be negative")
    private int loyaltyPointsToRedeem = 0;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemRequest {
        @NotNull(message = "Product is required")
        private Long productId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;

        @DecimalMin(value = "0.0", message = "Discount cannot be negative")
        private BigDecimal discountAmount = BigDecimal.ZERO;
    }
}
