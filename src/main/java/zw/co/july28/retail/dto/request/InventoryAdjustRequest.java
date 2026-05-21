package zw.co.july28.retail.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustRequest {

    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Branch is required")
    private Long branchId;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    private String reason;
}
