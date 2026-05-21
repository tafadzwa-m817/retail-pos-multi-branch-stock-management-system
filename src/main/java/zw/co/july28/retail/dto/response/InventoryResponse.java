package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.Inventory;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long branchId;
    private String branchName;
    private int quantity;
    private int reorderLevel;
    private boolean lowStock;
    private LocalDateTime lastUpdated;

    public static InventoryResponse from(Inventory inventory) {
        int reorderLevel = inventory.getProduct().getReorderLevel();
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .productSku(inventory.getProduct().getSku())
                .branchId(inventory.getBranch().getId())
                .branchName(inventory.getBranch().getName())
                .quantity(inventory.getQuantity())
                .reorderLevel(reorderLevel)
                .lowStock(inventory.getQuantity() <= reorderLevel)
                .lastUpdated(inventory.getLastUpdated())
                .build();
    }
}
