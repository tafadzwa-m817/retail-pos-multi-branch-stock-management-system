package zw.co.july28.retail.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.dto.response.InventoryResponse;
import zw.co.july28.retail.dto.response.SalesReportResponse;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfReportService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(21, 101, 192));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);

    public byte[] generateSalesReportPdf(SalesReportResponse report, LocalDateTime start, LocalDateTime end) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("july28 Retail — Sales Report", TITLE_FONT));
            doc.add(new Paragraph("Branch: " + report.getBranchName(), CELL_FONT));
            doc.add(new Paragraph("Period: " + fmt(start) + " to " + fmt(end), CELL_FONT));
            doc.add(new Paragraph("Generated: " + fmt(LocalDateTime.now()), SMALL_FONT));
            doc.add(Chunk.NEWLINE);

            // Summary
            PdfPTable summary = new PdfPTable(3);
            summary.setWidthPercentage(100);
            addColoredHeader(summary, "Total Transactions");
            addColoredHeader(summary, "Total Revenue");
            addColoredHeader(summary, "Avg Order Value");
            addCell(summary, String.valueOf(report.getTotalTransactions()));
            addCell(summary, "$" + String.format("%.2f", report.getTotalRevenue()));
            addCell(summary, "$" + String.format("%.2f", report.getAverageOrderValue()));
            doc.add(summary);
            doc.add(Chunk.NEWLINE);

            // Top products
            doc.add(new Paragraph("Top Products", new Font(Font.HELVETICA, 12, Font.BOLD)));
            doc.add(Chunk.NEWLINE);
            PdfPTable products = new PdfPTable(4);
            products.setWidthPercentage(100);
            products.setWidths(new float[]{0.5f, 3f, 1f, 1.5f});
            addColoredHeader(products, "#");
            addColoredHeader(products, "Product");
            addColoredHeader(products, "Qty Sold");
            addColoredHeader(products, "Revenue");
            int i = 1;
            for (SalesReportResponse.ProductSalesSummary p : report.getTopProducts()) {
                addCell(products, String.valueOf(i++));
                addCell(products, p.getProductName());
                addCell(products, String.valueOf(p.getQuantitySold()));
                addCell(products, "$" + String.format("%.2f", p.getRevenue()));
            }
            doc.add(products);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    public byte[] generateInventoryReportPdf(List<InventoryResponse> inventory, String branchName) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("july28 Retail — Inventory Report", TITLE_FONT));
            doc.add(new Paragraph("Branch: " + branchName, CELL_FONT));
            doc.add(new Paragraph("Generated: " + fmt(LocalDateTime.now()), SMALL_FONT));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1.5f, 2f, 1f, 1f, 1.5f});
            for (String h : new String[]{"Product", "SKU", "Branch", "Qty", "Reorder At", "Status"}) {
                addColoredHeader(table, h);
            }
            for (InventoryResponse item : inventory) {
                addCell(table, item.getProductName());
                addCell(table, item.getProductSku() != null ? item.getProductSku() : "—");
                addCell(table, item.getBranchName());
                PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), CELL_FONT));
                qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                if (item.isLowStock()) qtyCell.setBackgroundColor(new Color(255, 205, 210));
                table.addCell(qtyCell);
                addCell(table, String.valueOf(item.getReorderLevel()));
                addCell(table, item.isLowStock() ? "LOW STOCK" : "OK");
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private void addColoredHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new Color(21, 101, 192));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_FONT));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String fmt(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
    }
}
