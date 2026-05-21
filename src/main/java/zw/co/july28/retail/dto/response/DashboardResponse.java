package zw.co.july28.retail.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private int todaySalesCount;
    private BigDecimal todayRevenue;
    private BigDecimal todayAverageOrderValue;
    private int totalActiveProducts;
    private int lowStockItemCount;
    private int totalActiveBranches;
    private int totalActiveCustomers;
    private int totalActiveUsers;
    private List<BranchPerformance> branchPerformance;
    private List<TopProduct> topProductsToday;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BranchPerformance {
        private Long branchId;
        private String branchName;
        private int salesCount;
        private BigDecimal revenue;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProduct {
        private Long productId;
        private String productName;
        private int quantitySold;
        private BigDecimal revenue;
    }
}
