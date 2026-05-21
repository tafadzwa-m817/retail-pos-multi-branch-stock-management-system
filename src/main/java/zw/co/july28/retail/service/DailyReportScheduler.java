package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zw.co.july28.retail.entity.Inventory;
import zw.co.july28.retail.entity.Sale;
import zw.co.july28.retail.repository.InventoryRepository;
import zw.co.july28.retail.repository.SaleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportScheduler {

    private final SaleRepository saleRepository;
    private final InventoryRepository inventoryRepository;
    private final EmailService emailService;

    @Value("${admin.alert-email:admin@july28retail.co.zw}")
    private String adminEmail;

    /** Runs every day at 07:00 — sends yesterday's summary + low-stock digest */
    @Scheduled(cron = "0 0 7 * * *")
    public void sendDailyReport() {
        log.info("Running daily report scheduler...");
        try {
            LocalDateTime start = LocalDate.now().minusDays(1).atStartOfDay();
            LocalDateTime end   = LocalDate.now().atStartOfDay().minusSeconds(1);

            List<Sale> yesterdaySales = saleRepository.findByDateRange(start, end);
            List<Inventory> lowStock  = inventoryRepository.findAllLowStock();

            BigDecimal revenue = yesterdaySales.stream()
                    .map(Sale::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String subject = String.format("Daily Report — %s | %d sales, $%.2f revenue",
                    LocalDate.now().minusDays(1), yesterdaySales.size(), revenue);

            emailService.sendDailySummaryEmail(adminEmail, subject,
                    yesterdaySales.size(), revenue, lowStock);

            log.info("Daily report sent: {} sales, ${} revenue, {} low-stock items",
                    yesterdaySales.size(), revenue, lowStock.size());
        } catch (Exception e) {
            log.error("Daily report failed: {}", e.getMessage());
        }
    }
}
