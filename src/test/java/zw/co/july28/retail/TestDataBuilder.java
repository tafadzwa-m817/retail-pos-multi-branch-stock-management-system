package zw.co.july28.retail;

import zw.co.july28.retail.entity.*;
import zw.co.july28.retail.enums.*;

import java.math.BigDecimal;

/**
 * Factory for building test entities — keeps test classes lean.
 */
public final class TestDataBuilder {

    private TestDataBuilder() {}

    public static Branch branch(String name) {
        return Branch.builder().name(name).address("123 Test St").phone("+263 000").active(true).build();
    }

    public static AppUser superAdmin(String email, String encodedPassword) {
        return AppUser.builder()
                .firstName("Super").lastName("Admin")
                .email(email).password(encodedPassword)
                .role(Role.SUPER_ADMIN).active(true).build();
    }

    public static AppUser cashier(String email, String encodedPassword, Branch branch) {
        return AppUser.builder()
                .firstName("Test").lastName("Cashier")
                .email(email).password(encodedPassword)
                .role(Role.CASHIER).branch(branch).active(true).build();
    }

    public static AppUser branchManager(String email, String encodedPassword, Branch branch) {
        return AppUser.builder()
                .firstName("Branch").lastName("Manager")
                .email(email).password(encodedPassword)
                .role(Role.BRANCH_MANAGER).branch(branch).active(true).build();
    }

    public static Category category(String name) {
        return Category.builder().name(name).description("Test category").build();
    }

    public static Product product(String name, String sku, Category category, double costPrice, double sellingPrice) {
        return Product.builder()
                .name(name).sku(sku).barcode("BC-" + sku)
                .category(category)
                .costPrice(BigDecimal.valueOf(costPrice))
                .sellingPrice(BigDecimal.valueOf(sellingPrice))
                .reorderLevel(10).active(true).build();
    }

    public static Inventory inventory(Product product, Branch branch, int quantity) {
        return Inventory.builder().product(product).branch(branch).quantity(quantity).build();
    }

    public static Customer customer(String firstName, String lastName) {
        return Customer.builder()
                .firstName(firstName).lastName(lastName)
                .email(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@test.com")
                .phone("+263 777 000000")
                .loyaltyPoints(0).build();
    }

    public static Supplier supplier(String name) {
        return Supplier.builder().name(name).email(name.toLowerCase().replace(" ", "") + "@supplier.com")
                .phone("+263 000 000").active(true).build();
    }
}
