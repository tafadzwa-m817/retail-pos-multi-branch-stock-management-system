package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String sku;
    private String barcode;
    private String description;
    private Long categoryId;
    private String categoryName;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private int reorderLevel;
    private boolean active;
    private LocalDateTime createdAt;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .description(product.getDescription())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .reorderLevel(product.getReorderLevel())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
