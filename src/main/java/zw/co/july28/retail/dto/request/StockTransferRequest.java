package zw.co.july28.retail.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferRequest {

    @NotNull(message = "Source branch is required")
    private Long fromBranchId;

    @NotNull(message = "Destination branch is required")
    private Long toBranchId;

    private String notes;

    @NotEmpty(message = "At least one item is required")
    private List<TransferItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferItemRequest {
        @NotNull(message = "Product is required")
        private Long productId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;
    }
}
