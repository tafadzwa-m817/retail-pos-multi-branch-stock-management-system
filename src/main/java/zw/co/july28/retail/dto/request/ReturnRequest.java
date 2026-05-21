package zw.co.july28.retail.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {

    @NotNull(message = "Original sale ID is required")
    private Long originalSaleId;

    @NotBlank(message = "Return reason is required")
    private String reason;

    @NotEmpty(message = "At least one item must be returned")
    private List<ReturnItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemRequest {
        @NotNull(message = "Sale item ID is required")
        private Long saleItemId;

        @Min(value = 1, message = "Return quantity must be at least 1")
        private int quantity;
    }
}
