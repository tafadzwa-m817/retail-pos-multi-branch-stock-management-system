package zw.co.july28.retail.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportResponse {
    private int totalTransactions;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private String branchName;
    private String period;
    private List<ProductSalesSummary> topProducts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSalesSummary {
        private Long productId;
        private String productName;
        private int quantitySold;
        private BigDecimal revenue;
    }
}
