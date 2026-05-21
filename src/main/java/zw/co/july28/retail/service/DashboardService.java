package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.dto.response.DashboardResponse;
import zw.co.july28.retail.entity.Branch;
import zw.co.july28.retail.entity.Sale;
import zw.co.july28.retail.entity.SaleItem;
import zw.co.july28.retail.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final AppUserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    public DashboardResponse getDashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<Sale> todaySales = saleRepository.findByDateRange(startOfDay, now);
        List<Branch> activeBranches = branchRepository.findByActiveTrue();

        BigDecimal todayRevenue = todaySales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgOrderValue = todaySales.isEmpty() ? BigDecimal.ZERO
                : todayRevenue.divide(BigDecimal.valueOf(todaySales.size()), 2, RoundingMode.HALF_UP);

        int lowStockCount = inventoryRepository.findAllLowStock().size();
        int activeProducts = productRepository.findByActiveTrue().size();
        long activeCustomers = customerRepository.count();
        long activeUsers = userRepository.findByActiveTrue().size();

        // Per-branch performance today
        List<DashboardResponse.BranchPerformance> branchPerformance = activeBranches.stream()
                .map(branch -> {
                    List<Sale> branchSales = todaySales.stream()
                            .filter(s -> s.getBranch().getId().equals(branch.getId()))
                            .collect(Collectors.toList());
                    BigDecimal revenue = branchSales.stream()
                            .map(Sale::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return DashboardResponse.BranchPerformance.builder()
                            .branchId(branch.getId())
                            .branchName(branch.getName())
                            .salesCount(branchSales.size())
                            .revenue(revenue)
                            .build();
                })
                .sorted(Comparator.comparing(DashboardResponse.BranchPerformance::getRevenue).reversed())
                .collect(Collectors.toList());

        // Top products today
        Map<Long, DashboardResponse.TopProduct> productMap = new HashMap<>();
        for (Sale sale : todaySales) {
            for (SaleItem item : sale.getItems()) {
                Long productId = item.getProduct().getId();
                DashboardResponse.TopProduct current = productMap.getOrDefault(productId,
                        DashboardResponse.TopProduct.builder()
                                .productId(productId)
                                .productName(item.getProduct().getName())
                                .quantitySold(0)
                                .revenue(BigDecimal.ZERO)
                                .build());
                productMap.put(productId, DashboardResponse.TopProduct.builder()
                        .productId(productId)
                        .productName(item.getProduct().getName())
                        .quantitySold(current.getQuantitySold() + item.getQuantity())
                        .revenue(current.getRevenue().add(item.getTotalPrice()))
                        .build());
            }
        }

        List<DashboardResponse.TopProduct> topProducts = productMap.values().stream()
                .sorted(Comparator.comparing(DashboardResponse.TopProduct::getRevenue).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .todaySalesCount(todaySales.size())
                .todayRevenue(todayRevenue)
                .todayAverageOrderValue(avgOrderValue)
                .totalActiveProducts(activeProducts)
                .lowStockItemCount(lowStockCount)
                .totalActiveBranches(activeBranches.size())
                .totalActiveCustomers((int) activeCustomers)
                .totalActiveUsers((int) activeUsers)
                .branchPerformance(branchPerformance)
                .topProductsToday(topProducts)
                .build();
    }
}
