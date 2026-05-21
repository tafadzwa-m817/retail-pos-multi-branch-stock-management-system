package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.entity.Inventory;
import zw.co.july28.retail.entity.PurchaseOrder;
import zw.co.july28.retail.entity.Sale;
import zw.co.july28.retail.entity.SaleItem;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final Optional<JavaMailSender> mailSender;

    @Value("${spring.mail.from:noreply@july28retail.co.zw}")
    private String fromEmail;

    @Value("${spring.mail.enabled:false}")
    private boolean enabled;

    @Async
    public void sendReceiptEmail(Sale sale) {
        if (!enabled || !mailSender.isPresent()) {
            log.debug("Email not configured — skipping receipt email for sale #{}", sale.getId());
            return;
        }
        if (sale.getCustomer() == null || sale.getCustomer().getEmail() == null) return;

        try {
            var message = mailSender.get().createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(sale.getCustomer().getEmail());
            helper.setSubject("Your Receipt — Sale #" + sale.getId() + " | july28 Retail");
            helper.setText(buildReceiptHtml(sale), true);
            mailSender.get().send(message);
            log.info("Receipt email sent for sale #{} to {}", sale.getId(), sale.getCustomer().getEmail());
        } catch (Exception e) {
            log.error("Failed to send receipt email for sale #{}: {}", sale.getId(), e.getMessage());
        }
    }

    @Async
    public void sendLowStockAlert(List<Inventory> lowStockItems, String adminEmail) {
        if (!enabled || !mailSender.isPresent() || lowStockItems.isEmpty()) return;

        try {
            var message = mailSender.get().createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("Low Stock Alert — " + lowStockItems.size() + " items need restocking");
            helper.setText(buildLowStockHtml(lowStockItems), true);
            mailSender.get().send(message);
            log.info("Low stock alert sent to {}", adminEmail);
        } catch (Exception e) {
            log.error("Failed to send low stock alert: {}", e.getMessage());
        }
    }

    @Async
    public void sendPurchaseOrderEmail(PurchaseOrder order) {
        if (!enabled || !mailSender.isPresent()) return;
        if (order.getSupplier().getEmail() == null) return;

        try {
            var message = mailSender.get().createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(order.getSupplier().getEmail());
            helper.setSubject("Purchase Order #" + order.getId() + " — july28 Retail");
            helper.setText(buildPurchaseOrderHtml(order), true);
            mailSender.get().send(message);
            log.info("PO email sent to supplier {} for order #{}", order.getSupplier().getName(), order.getId());
        } catch (Exception e) {
            log.error("Failed to send PO email: {}", e.getMessage());
        }
    }

    private String buildReceiptHtml(Sale sale) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;max-width:500px;margin:auto;padding:20px;border:1px solid #eee;border-radius:8px'>");
        sb.append("<h2 style='color:#1565C0;text-align:center'>july28 Retail</h2>");
        sb.append("<p style='text-align:center;color:#666'>Thank you for your purchase!</p>");
        sb.append("<hr/>");
        sb.append("<p><strong>Receipt #</strong> ").append(sale.getId()).append("</p>");
        sb.append("<p><strong>Branch:</strong> ").append(sale.getBranch().getName()).append("</p>");
        sb.append("<p><strong>Date:</strong> ").append(sale.getCreatedAt()).append("</p>");
        sb.append("<hr/><table width='100%' cellpadding='6'>");
        sb.append("<tr style='background:#f5f5f5'><th align='left'>Item</th><th align='right'>Qty</th><th align='right'>Price</th></tr>");
        for (SaleItem item : sale.getItems()) {
            sb.append("<tr><td>").append(item.getProduct().getName()).append("</td>");
            sb.append("<td align='right'>").append(item.getQuantity()).append("</td>");
            sb.append("<td align='right'>$").append(String.format("%.2f", item.getTotalPrice())).append("</td></tr>");
        }
        sb.append("</table><hr/>");
        sb.append("<p style='text-align:right'><strong>Total: $").append(String.format("%.2f", sale.getTotalAmount())).append("</strong></p>");
        sb.append("<p style='text-align:right;color:#666'>Payment: ").append(sale.getPaymentMethod()).append("</p>");
        if (sale.getCustomer() != null) {
            sb.append("<p style='color:#2E7D32'>Loyalty points earned: +").append((int) sale.getTotalAmount().doubleValue()).append("</p>");
        }
        sb.append("<hr/><p style='text-align:center;color:#999;font-size:12px'>july28 Systems · Powered by RetailPOS</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private String buildLowStockHtml(List<Inventory> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px'>");
        sb.append("<h2 style='color:#C62828'>Low Stock Alert</h2>");
        sb.append("<p>The following items have reached or fallen below their reorder level:</p>");
        sb.append("<table width='100%' cellpadding='6' style='border-collapse:collapse'>");
        sb.append("<tr style='background:#f5f5f5'><th>Product</th><th>Branch</th><th>Current Qty</th><th>Reorder At</th></tr>");
        for (Inventory inv : items) {
            sb.append("<tr style='border-bottom:1px solid #eee'>");
            sb.append("<td>").append(inv.getProduct().getName()).append("</td>");
            sb.append("<td>").append(inv.getBranch().getName()).append("</td>");
            sb.append("<td style='color:red;font-weight:bold'>").append(inv.getQuantity()).append("</td>");
            sb.append("<td>").append(inv.getProduct().getReorderLevel()).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        sb.append("<p style='color:#999;font-size:12px;margin-top:20px'>july28 RetailPOS — Automated Alert</p>");
        sb.append("</div>");
        return sb.toString();
    }

    @Async
    public void sendDailySummaryEmail(String to, String subject, int salesCount,
                                       java.math.BigDecimal revenue, List<Inventory> lowStock) {
        if (!enabled || !mailSender.isPresent()) {
            log.debug("Email not configured — skipping daily summary");
            return;
        }
        try {
            var message = mailSender.get().createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            StringBuilder sb = new StringBuilder();
            sb.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px'>");
            sb.append("<h2 style='color:#1565C0'>Daily Report — july28 Retail</h2>");
            sb.append("<h3>Yesterday's Summary</h3>");
            sb.append("<p><strong>Total Sales:</strong> ").append(salesCount).append("</p>");
            sb.append("<p><strong>Total Revenue:</strong> $").append(String.format("%.2f", revenue)).append("</p>");
            if (!lowStock.isEmpty()) {
                sb.append("<h3 style='color:#C62828'>Low Stock Alert — ").append(lowStock.size()).append(" items</h3>");
                sb.append("<table width='100%' cellpadding='4' style='border-collapse:collapse'>");
                sb.append("<tr style='background:#f5f5f5'><th>Product</th><th>Branch</th><th>Qty</th><th>Reorder At</th></tr>");
                lowStock.forEach(inv -> sb.append("<tr>")
                        .append("<td>").append(inv.getProduct().getName()).append("</td>")
                        .append("<td>").append(inv.getBranch().getName()).append("</td>")
                        .append("<td style='color:red;font-weight:bold'>").append(inv.getQuantity()).append("</td>")
                        .append("<td>").append(inv.getProduct().getReorderLevel()).append("</td>")
                        .append("</tr>"));
                sb.append("</table>");
            }
            sb.append("<p style='color:#999;font-size:12px;margin-top:20px'>july28 RetailPOS — Scheduled Daily Report</p>");
            sb.append("</div>");

            helper.setText(sb.toString(), true);
            mailSender.get().send(message);
            log.info("Daily summary email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send daily summary email: {}", e.getMessage());
        }
    }

    private String buildPurchaseOrderHtml(PurchaseOrder order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px;border:1px solid #eee'>");
        sb.append("<h2 style='color:#1565C0'>Purchase Order #").append(order.getId()).append("</h2>");
        sb.append("<p><strong>From:</strong> july28 Retail — ").append(order.getBranch().getName()).append("</p>");
        sb.append("<p><strong>To:</strong> ").append(order.getSupplier().getName()).append("</p>");
        sb.append("<p><strong>Date:</strong> ").append(order.getCreatedAt()).append("</p>");
        sb.append("<hr/><table width='100%' cellpadding='6' style='border-collapse:collapse'>");
        sb.append("<tr style='background:#f5f5f5'><th align='left'>Product</th><th align='right'>Qty</th><th align='right'>Unit Cost</th><th align='right'>Total</th></tr>");
        order.getItems().forEach(item -> {
            sb.append("<tr><td>").append(item.getProduct().getName()).append("</td>");
            sb.append("<td align='right'>").append(item.getQuantity()).append("</td>");
            sb.append("<td align='right'>$").append(String.format("%.2f", item.getUnitCost())).append("</td>");
            sb.append("<td align='right'>$").append(String.format("%.2f", item.getTotalCost())).append("</td></tr>");
        });
        sb.append("</table><hr/>");
        sb.append("<p style='text-align:right'><strong>Order Total: $").append(String.format("%.2f", order.getTotalAmount())).append("</strong></p>");
        if (order.getNotes() != null) sb.append("<p><strong>Notes:</strong> ").append(order.getNotes()).append("</p>");
        sb.append("</div>");
        return sb.toString();
    }
}
