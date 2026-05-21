package zw.co.july28.retail.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import zw.co.july28.retail.entity.*;
import zw.co.july28.retail.enums.Role;
import zw.co.july28.retail.repository.*;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;
    private final TaxRateRepository taxRateRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // Branches
        Branch mainBranch = branchRepository.save(Branch.builder()
                .name("Main Branch - Harare CBD")
                .address("123 Samora Machel Ave, Harare")
                .phone("+263 242 123456")
                .email("harare@july28retail.co.zw")
                .active(true).build());

        Branch northBranch = branchRepository.save(Branch.builder()
                .name("North Branch - Borrowdale")
                .address("45 Enterprise Road, Borrowdale")
                .phone("+263 242 654321")
                .email("borrowdale@july28retail.co.zw")
                .active(true).build());

        Branch bulawayoBranch = branchRepository.save(Branch.builder()
                .name("Bulawayo Branch")
                .address("78 Jason Moyo St, Bulawayo")
                .phone("+263 292 112233")
                .email("bulawayo@july28retail.co.zw")
                .active(true).build());

        // Users
        userRepository.save(AppUser.builder()
                .firstName("Super").lastName("Admin")
                .email("admin@july28retail.co.zw")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.SUPER_ADMIN).active(true).build());

        userRepository.save(AppUser.builder()
                .firstName("Tafadzwa").lastName("Manager")
                .email("manager@july28retail.co.zw")
                .password(passwordEncoder.encode("manager123"))
                .role(Role.BRANCH_MANAGER).branch(mainBranch).active(true).build());

        AppUser cashier = userRepository.save(AppUser.builder()
                .firstName("Chipo").lastName("Cashier")
                .email("cashier@july28retail.co.zw")
                .password(passwordEncoder.encode("cashier123"))
                .role(Role.CASHIER).branch(mainBranch).active(true).build());

        // Categories
        Category electronics = categoryRepository.save(Category.builder()
                .name("Electronics").description("Electronic devices and accessories").build());
        Category clothing = categoryRepository.save(Category.builder()
                .name("Clothing").description("Men's and women's clothing").build());
        Category food = categoryRepository.save(Category.builder()
                .name("Food & Beverages").description("Groceries and drinks").build());
        Category household = categoryRepository.save(Category.builder()
                .name("Household").description("Home and kitchen items").build());

        // Suppliers
        Supplier techSupplier = supplierRepository.save(Supplier.builder()
                .name("TechZim Distributors").contactPerson("John Moyo")
                .email("john@techzim.co.zw").phone("+263 772 111222")
                .address("Msasa Industrial, Harare").active(true).build());

        Supplier grocerySupplier = supplierRepository.save(Supplier.builder()
                .name("Fresh Foods Ltd").contactPerson("Mary Ndlovu")
                .email("mary@freshfoods.co.zw").phone("+263 772 333444")
                .address("Workington, Harare").active(true).build());

        // Products
        List<Product> products = productRepository.saveAll(List.of(
                Product.builder().name("Samsung 65\" 4K TV").sku("SAM-TV-65-4K").barcode("6001234567890")
                        .category(electronics).costPrice(new BigDecimal("450.00"))
                        .sellingPrice(new BigDecimal("650.00")).reorderLevel(5).active(true).build(),
                Product.builder().name("iPhone 15 Case").sku("IPH-CASE-15").barcode("6001234567891")
                        .category(electronics).costPrice(new BigDecimal("8.00"))
                        .sellingPrice(new BigDecimal("15.00")).reorderLevel(20).active(true).build(),
                Product.builder().name("USB-C Charging Cable 1m").sku("USB-C-1M").barcode("6001234567892")
                        .category(electronics).costPrice(new BigDecimal("3.00"))
                        .sellingPrice(new BigDecimal("7.00")).reorderLevel(30).active(true).build(),
                Product.builder().name("Men's Polo Shirt (M)").sku("POL-M-MEDIUM").barcode("6001234567893")
                        .category(clothing).costPrice(new BigDecimal("12.00"))
                        .sellingPrice(new BigDecimal("25.00")).reorderLevel(15).active(true).build(),
                Product.builder().name("Ladies Dress - Blue (L)").sku("DRESS-BLU-L").barcode("6001234567894")
                        .category(clothing).costPrice(new BigDecimal("18.00"))
                        .sellingPrice(new BigDecimal("40.00")).reorderLevel(10).active(true).build(),
                Product.builder().name("Coca-Cola 500ml").sku("COKE-500ML").barcode("6001234567895")
                        .category(food).costPrice(new BigDecimal("0.60"))
                        .sellingPrice(new BigDecimal("1.00")).reorderLevel(100).active(true).build(),
                Product.builder().name("Bread Loaf 700g").sku("BREAD-700G").barcode("6001234567896")
                        .category(food).costPrice(new BigDecimal("0.90"))
                        .sellingPrice(new BigDecimal("1.50")).reorderLevel(50).active(true).build(),
                Product.builder().name("Rice 2kg").sku("RICE-2KG").barcode("6001234567897")
                        .category(food).costPrice(new BigDecimal("1.80"))
                        .sellingPrice(new BigDecimal("3.00")).reorderLevel(40).active(true).build(),
                Product.builder().name("Electric Kettle 1.7L").sku("KETTLE-1.7L").barcode("6001234567898")
                        .category(household).costPrice(new BigDecimal("22.00"))
                        .sellingPrice(new BigDecimal("35.00")).reorderLevel(8).active(true).build(),
                Product.builder().name("Mop & Bucket Set").sku("MOP-SET-01").barcode("6001234567899")
                        .category(household).costPrice(new BigDecimal("10.00"))
                        .sellingPrice(new BigDecimal("18.00")).reorderLevel(12).active(true).build()
        ));

        // Seed inventory for each branch
        for (Product product : products) {
            inventoryRepository.save(Inventory.builder()
                    .product(product).branch(mainBranch).quantity(100).build());
            inventoryRepository.save(Inventory.builder()
                    .product(product).branch(northBranch).quantity(50).build());
            inventoryRepository.save(Inventory.builder()
                    .product(product).branch(bulawayoBranch).quantity(30).build());
        }

        // Seed a default VAT tax rate
        taxRateRepository.save(TaxRate.builder()
                .name("VAT 15%").rate(new BigDecimal("15.00")).active(true).build());

        // Seed expense categories
        List.of(
                new String[]{"Rent", "#1565C0"},
                new String[]{"Utilities", "#FF8F00"},
                new String[]{"Salaries", "#2E7D32"},
                new String[]{"Stock Losses", "#C62828"},
                new String[]{"Marketing", "#6A1B9A"},
                new String[]{"Other", "#757575"}
        ).forEach(e -> expenseCategoryRepository.save(
                ExpenseCategory.builder().name(e[0]).color(e[1]).build()));

        // Seed default store settings (singleton)
        storeSettingsRepository.save(StoreSettings.builder()
                .id(1L)
                .storeName("july28 Retail")
                .storePhone("+263 242 123456")
                .storeAddress("Harare, Zimbabwe")
                .receiptFooterText("Thank you for shopping with july28 Retail!")
                .currency("USD")
                .build());

        log.info("=============================================================");
        log.info("  Retail POS & Stock Management System started successfully");
        log.info("  Swagger UI: http://localhost:8080/swagger-ui/index.html");
        log.info("  H2 Console:  http://localhost:8080/h2-console");
        log.info("  Default credentials:");
        log.info("    Super Admin: admin@july28retail.co.zw / admin123");
        log.info("    Manager:     manager@july28retail.co.zw / manager123");
        log.info("    Cashier:     cashier@july28retail.co.zw / cashier123");
        log.info("=============================================================");
    }
}
