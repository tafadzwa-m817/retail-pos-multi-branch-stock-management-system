package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.Promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse {
    private Long id;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal minimumPurchaseAmount;
    private boolean applyToAll;
    private List<Long> applicableProductIds;
    private List<String> applicableProductNames;
    private boolean active;
    private boolean currentlyActive;
    private LocalDateTime createdAt;

    public static PromotionResponse from(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .discountType(promotion.getDiscountType().name())
                .discountValue(promotion.getDiscountValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .minimumPurchaseAmount(promotion.getMinimumPurchaseAmount())
                .applyToAll(promotion.isApplyToAll())
                .applicableProductIds(promotion.getApplicableProducts().stream()
                        .map(p -> p.getId()).collect(Collectors.toList()))
                .applicableProductNames(promotion.getApplicableProducts().stream()
                        .map(p -> p.getName()).collect(Collectors.toList()))
                .active(promotion.isActive())
                .currentlyActive(promotion.isCurrentlyActive())
                .createdAt(promotion.getCreatedAt())
                .build();
    }
}
