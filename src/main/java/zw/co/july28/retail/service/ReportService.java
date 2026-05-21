package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.dto.response.InventoryResponse;
import zw.co.july28.retail.dto.response.SalesReportResponse;
import zw.co.july28.retail.entity.Sale;
import zw.co.july28.retail.entity.SaleItem;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.BranchRepository;
import zw.co.july28.retail.repository.SaleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository saleRepository;
    private final BranchRepository branchRepository;
    private final InventoryService inventoryService;

    public SalesReportResponse getSalesReport(Long branchId, LocalDateTime start, LocalDateTime end) {
        List<Sale> sales = branchId != null
                ? saleRepository.findByBranchAndDateRange(branchId, start, end)
                : saleRepository.findByDateRange(start, end);

        String branchName = branchId != null
                ? branchRepository.findById(branchId)
                        .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId))
                        .getName()
                : "All Branches";

        BigDecimal totalRevenue = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgOrderValue = sales.isEmpty() ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);

        Map<Long, SalesReportResponse.ProductSalesSummary> productMap = new HashMap<>();
        for (Sale sale : sales) {
            for (SaleItem item : sale.getItems()) {
                Long productId = item.getProduct().getId();
                SalesReportResponse.ProductSalesSummary summary = productMap.computeIfAbsent(productId, k ->
                        SalesReportResponse.ProductSalesSummary.builder()
                                .productId(productId)
                                .productName(item.getProduct().getName())
                                .quantitySold(0)
                                .revenue(BigDecimal.ZERO)
                                .build()
                );
                productMap.put(productId, SalesReportResponse.ProductSalesSummary.builder()
                        .productId(productId)
                        .productName(item.getProduct().getName())
                        .quantitySold(summary.getQuantitySold() + item.getQuantity())
                        .revenue(summary.getRevenue().add(item.getTotalPrice()))
                        .build());
            }
        }

        List<SalesReportResponse.ProductSalesSummary> topProducts = productMap.values().stream()
                .sorted(Comparator.comparing(SalesReportResponse.ProductSalesSummary::getRevenue).reversed())
                .limit(10)
                .collect(Collectors.toList());

        return SalesReportResponse.builder()
                .totalTransactions(sales.size())
                .totalRevenue(totalRevenue)
                .averageOrderValue(avgOrderValue)
                .branchName(branchName)
                .period(start.toLocalDate() + " to " + end.toLocalDate())
                .topProducts(topProducts)
                .build();
    }

    public List<InventoryResponse> getInventoryReport(Long branchId) {
        return inventoryService.getInventoryByBranch(branchId);
    }

    public List<InventoryResponse> getLowStockReport(Long branchId) {
        return inventoryService.getLowStockItems(branchId);
    }

    public record DailySummary(String date, int salesCount, BigDecimal revenue) {}

    public List<DailySummary> getDailySummary(int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = LocalDateTime.now().minusDays(days).toLocalDate().atStartOfDay();
        List<Sale> sales = saleRepository.findByDateRange(start, end);

        java.util.Map<LocalDate, List<Sale>> byDate = sales.stream()
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().toLocalDate()));

        List<DailySummary> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<Sale> daySales = byDate.getOrDefault(date, List.of());
            BigDecimal revenue = daySales.stream()
                    .map(Sale::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new DailySummary(date.toString(), daySales.size(), revenue));
        }
        return result;
    }
}
