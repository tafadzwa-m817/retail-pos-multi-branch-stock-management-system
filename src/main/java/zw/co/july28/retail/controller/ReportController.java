package zw.co.july28.retail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import zw.co.july28.retail.dto.response.ApiResponse;
import zw.co.july28.retail.dto.response.InventoryResponse;
import zw.co.july28.retail.dto.response.SaleResponse;
import zw.co.july28.retail.dto.response.SalesReportResponse;
import zw.co.july28.retail.service.ExcelReportService;
import zw.co.july28.retail.service.InventoryService;
import zw.co.july28.retail.service.PdfReportService;
import zw.co.july28.retail.service.ReportService;
import zw.co.july28.retail.service.SaleService;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER')")
@Tag(name = "Reports", description = "Business analytics, reporting, and CSV exports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;
    private final SaleService saleService;
    private final PdfReportService pdfReportService;
    private final ExcelReportService excelReportService;
    private final InventoryService inventoryService;

    @GetMapping("/sales")
    @Operation(summary = "Sales report with top products")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestParam(required = false) Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getSalesReport(branchId, start, end)));
    }

    @GetMapping("/inventory/{branchId}")
    @Operation(summary = "Full inventory report for a branch")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryReport(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getInventoryReport(branchId)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Low stock alert report")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStockReport(
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getLowStockReport(branchId)));
    }

    @GetMapping("/reorder-suggestions")
    @Operation(summary = "List all items below reorder level — for generating purchase orders")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getReorderSuggestions(
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getLowStockReport(branchId)));
    }

    @GetMapping("/sales/excel")
    @Operation(summary = "Download sales report as Excel (.xlsx)")
    public ResponseEntity<byte[]> downloadSalesExcel(
            @RequestParam(required = false) Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        var report = reportService.getSalesReport(branchId, start, end);
        byte[] excel = excelReportService.generateSalesExcel(report, start, end);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales-report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/inventory/{branchId}/excel")
    @Operation(summary = "Download inventory report as Excel (.xlsx)")
    public ResponseEntity<byte[]> downloadInventoryExcel(@PathVariable Long branchId) {
        var items = reportService.getInventoryReport(branchId);
        String branchName = items.isEmpty() ? "Branch" : items.get(0).getBranchName();
        byte[] excel = excelReportService.generateInventoryExcel(items, branchName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory-report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/sales/export")
    @Operation(summary = "Export sales to CSV")
    public void exportSalesCSV(
            @RequestParam(required = false) Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"sales-report.csv\"");

        List<SaleResponse> sales = saleService.getSalesByDateRange(branchId, start, end);

        PrintWriter writer = response.getWriter();
        writer.println("ID,Branch,Cashier,Customer,Subtotal,Discount,Total,Payment Method,Status,Date");
        for (SaleResponse sale : sales) {
            writer.printf("%d,\"%s\",\"%s\",\"%s\",%.2f,%.2f,%.2f,%s,%s,%s%n",
                    sale.getId(),
                    escape(sale.getBranchName()),
                    escape(sale.getCashierName()),
                    escape(sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in"),
                    sale.getSubtotal(),
                    sale.getDiscountAmount(),
                    sale.getTotalAmount(),
                    sale.getPaymentMethod(),
                    sale.getStatus(),
                    sale.getCreatedAt()
            );
        }
        writer.flush();
    }

    @GetMapping("/inventory/export")
    @Operation(summary = "Export inventory to CSV")
    public void exportInventoryCSV(
            @RequestParam Long branchId,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"inventory-report.csv\"");

        List<InventoryResponse> items = reportService.getInventoryReport(branchId);

        PrintWriter writer = response.getWriter();
        writer.println("Product ID,Product Name,SKU,Branch,Quantity,Reorder Level,Low Stock,Last Updated");
        for (InventoryResponse item : items) {
            writer.printf("%d,\"%s\",\"%s\",\"%s\",%d,%d,%s,%s%n",
                    item.getProductId(),
                    escape(item.getProductName()),
                    escape(item.getProductSku() != null ? item.getProductSku() : ""),
                    escape(item.getBranchName()),
                    item.getQuantity(),
                    item.getReorderLevel(),
                    item.isLowStock() ? "YES" : "NO",
                    item.getLastUpdated()
            );
        }
        writer.flush();
    }

    @GetMapping("/daily-summary")
    @Operation(summary = "Daily sales summary for charts — last N days")
    public ResponseEntity<ApiResponse<List<ReportService.DailySummary>>> getDailySummary(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getDailySummary(days)));
    }

    @GetMapping("/sales/pdf")
    @Operation(summary = "Download sales report as PDF")
    public ResponseEntity<byte[]> downloadSalesPdf(
            @RequestParam(required = false) Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        var report = reportService.getSalesReport(branchId, start, end);
        byte[] pdf = pdfReportService.generateSalesReportPdf(report, start, end);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/inventory/{branchId}/pdf")
    @Operation(summary = "Download inventory report as PDF")
    public ResponseEntity<byte[]> downloadInventoryPdf(@PathVariable Long branchId) {
        var items = reportService.getInventoryReport(branchId);
        var branchName = items.isEmpty() ? "Branch" : items.get(0).getBranchName();
        byte[] pdf = pdfReportService.generateInventoryReportPdf(items, branchName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
