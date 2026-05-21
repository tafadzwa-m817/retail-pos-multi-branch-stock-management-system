package zw.co.july28.retail.service;

import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import zw.co.july28.retail.entity.Inventory;
import zw.co.july28.retail.entity.Product;
import zw.co.july28.retail.repository.*;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;

    public record ImportResult(int created, int updated, int skipped, List<String> errors) {}

    /**
     * CSV format: name,sku,barcode,categoryName,costPrice,sellingPrice,reorderLevel
     */
    @Transactional
    public ImportResult importProducts(MultipartFile file) {
        int created = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        int row = 1;

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext(); // skip header row
            String[] line;
            while ((line = reader.readNext()) != null) {
                row++;
                if (line.length < 6) { errors.add("Row " + row + ": insufficient columns"); skipped++; continue; }
                try {
                    String name = line[0].trim();
                    String sku = line[1].trim();
                    String barcode = line[2].trim();
                    String catName = line[3].trim();
                    BigDecimal cost = new BigDecimal(line[4].trim());
                    BigDecimal sell = new BigDecimal(line[5].trim());
                    int reorder = line.length > 6 ? Integer.parseInt(line[6].trim()) : 10;

                    var category = categoryRepository.findByName(catName)
                            .orElse(null);
                    if (category == null) { errors.add("Row " + row + ": category '" + catName + "' not found"); skipped++; continue; }

                    // Try to find existing by SKU or barcode
                    var existing = sku.isEmpty() ? java.util.Optional.<Product>empty() : productRepository.findBySku(sku);
                    if (existing.isEmpty() && !barcode.isEmpty()) existing = productRepository.findByBarcode(barcode);

                    if (existing.isPresent()) {
                        Product p = existing.get();
                        p.setName(name); p.setCostPrice(cost); p.setSellingPrice(sell);
                        p.setReorderLevel(reorder); p.setCategory(category);
                        productRepository.save(p);
                        updated++;
                    } else {
                        productRepository.save(Product.builder()
                                .name(name).sku(sku.isEmpty() ? null : sku).barcode(barcode.isEmpty() ? null : barcode)
                                .category(category).costPrice(cost).sellingPrice(sell)
                                .reorderLevel(reorder).active(true).build());
                        created++;
                    }
                } catch (Exception e) {
                    errors.add("Row " + row + ": " + e.getMessage());
                    skipped++;
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read file: " + e.getMessage());
        }
        log.info("Product CSV import: created={}, updated={}, skipped={}", created, updated, skipped);
        return new ImportResult(created, updated, skipped, errors);
    }

    /**
     * CSV format: productSku,branchId,quantity
     */
    @Transactional
    public ImportResult importInventory(MultipartFile file) {
        int updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        int row = 1;

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            reader.readNext();
            String[] line;
            while ((line = reader.readNext()) != null) {
                row++;
                if (line.length < 3) { errors.add("Row " + row + ": need sku,branchId,quantity"); skipped++; continue; }
                try {
                    String sku = line[0].trim();
                    Long branchId = Long.parseLong(line[1].trim());
                    int qty = Integer.parseInt(line[2].trim());

                    var product = productRepository.findBySku(sku).orElse(null);
                    if (product == null) { errors.add("Row " + row + ": product SKU '" + sku + "' not found"); skipped++; continue; }
                    var branch = branchRepository.findById(branchId).orElse(null);
                    if (branch == null) { errors.add("Row " + row + ": branch ID " + branchId + " not found"); skipped++; continue; }

                    var inv = inventoryRepository.findByProductIdAndBranchId(product.getId(), branchId)
                            .orElse(Inventory.builder().product(product).branch(branch).build());
                    inv.setQuantity(qty);
                    inventoryRepository.save(inv);
                    updated++;
                } catch (Exception e) {
                    errors.add("Row " + row + ": " + e.getMessage());
                    skipped++;
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read file: " + e.getMessage());
        }
        log.info("Inventory CSV import: updated={}, skipped={}", updated, skipped);
        return new ImportResult(0, updated, skipped, errors);
    }
}
