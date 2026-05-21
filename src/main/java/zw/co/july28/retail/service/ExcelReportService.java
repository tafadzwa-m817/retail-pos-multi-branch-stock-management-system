package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.dto.response.InventoryResponse;
import zw.co.july28.retail.dto.response.SalesReportResponse;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelReportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public byte[] generateSalesExcel(SalesReportResponse report, LocalDateTime start, LocalDateTime end) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            // ── Summary sheet ─────────────────────────────────────────────────
            Sheet summary = wb.createSheet("Summary");
            Row title = summary.createRow(0);
            title.createCell(0).setCellValue("july28 Retail — Sales Report");
            title.createCell(2).setCellValue("Branch: " + report.getBranchName());
            summary.createRow(1).createCell(0).setCellValue("Period: " + start.format(FMT) + " to " + end.format(FMT));

            Row headers = summary.createRow(3);
            String[] cols = {"Metric", "Value"};
            for (int i = 0; i < cols.length; i++) { Cell c = headers.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(headerStyle); }

            int r = 4;
            addRow(summary, r++, "Total Transactions", String.valueOf(report.getTotalTransactions()));
            addRow(summary, r++, "Total Revenue ($)", String.format("%.2f", report.getTotalRevenue()));
            addRow(summary, r++, "Average Order Value ($)", String.format("%.2f", report.getAverageOrderValue()));
            summary.autoSizeColumn(0);
            summary.autoSizeColumn(1);

            // ── Top Products sheet ────────────────────────────────────────────
            Sheet products = wb.createSheet("Top Products");
            Row ph = products.createRow(0);
            String[] pcols = {"#", "Product", "Qty Sold", "Revenue ($)"};
            for (int i = 0; i < pcols.length; i++) { Cell c = ph.createCell(i); c.setCellValue(pcols[i]); c.setCellStyle(headerStyle); }

            int pr = 1;
            for (SalesReportResponse.ProductSalesSummary p : report.getTopProducts()) {
                Row row = products.createRow(pr++);
                row.createCell(0).setCellValue(pr - 1);
                row.createCell(1).setCellValue(p.getProductName());
                row.createCell(2).setCellValue(p.getQuantitySold());
                Cell rev = row.createCell(3); rev.setCellValue(p.getRevenue().doubleValue()); rev.setCellStyle(currencyStyle);
            }
            for (int i = 0; i < 4; i++) products.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel generation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate Excel: " + e.getMessage());
        }
    }

    public byte[] generateInventoryExcel(List<InventoryResponse> inventory, String branchName) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle lowStyle    = createLowStockStyle(wb);

            Sheet sheet = wb.createSheet("Inventory");
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("july28 Retail — Inventory Report: " + branchName);

            Row headers = sheet.createRow(2);
            String[] cols = {"Product", "SKU", "Branch", "Qty", "Reorder At", "Status", "Last Updated"};
            for (int i = 0; i < cols.length; i++) { Cell c = headers.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(headerStyle); }

            int r = 3;
            for (InventoryResponse item : inventory) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(item.getProductName());
                row.createCell(1).setCellValue(item.getProductSku() != null ? item.getProductSku() : "");
                row.createCell(2).setCellValue(item.getBranchName());
                Cell qty = row.createCell(3); qty.setCellValue(item.getQuantity());
                if (item.isLowStock()) qty.setCellStyle(lowStyle);
                row.createCell(4).setCellValue(item.getReorderLevel());
                row.createCell(5).setCellValue(item.isLowStock() ? "LOW STOCK" : "OK");
                row.createCell(6).setCellValue(item.getLastUpdated() != null ? item.getLastUpdated().toString() : "");
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel inventory generation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate Excel: " + e.getMessage());
        }
    }

    private void addRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("$#,##0.00"));
        return style;
    }

    private CellStyle createLowStockStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }
}
