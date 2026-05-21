package zw.co.july28.retail.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import zw.co.july28.retail.entity.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("InventoryRepository @DataJpaTest")
class InventoryRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired InventoryRepository inventoryRepository;

    private Branch branch;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        Category cat = em.persistFlushFind(Category.builder().name("Cat").description("d").build());
        branch = em.persistFlushFind(Branch.builder().name("Main").address("HRE").active(true).build());

        productA = em.persistFlushFind(Product.builder()
                .name("Product A").sku("PA").barcode("BCA").category(cat)
                .costPrice(BigDecimal.ONE).sellingPrice(BigDecimal.TEN).reorderLevel(10).active(true).build());
        productB = em.persistFlushFind(Product.builder()
                .name("Product B").sku("PB").barcode("BCB").category(cat)
                .costPrice(BigDecimal.ONE).sellingPrice(BigDecimal.TEN).reorderLevel(5).active(true).build());
    }

    @Test
    @DisplayName("findByProductIdAndBranchId: returns correct inventory record")
    void findByProductIdAndBranchId_returnsRecord() {
        em.persistFlushFind(Inventory.builder().product(productA).branch(branch).quantity(50).build());

        Optional<Inventory> result = inventoryRepository.findByProductIdAndBranchId(productA.getId(), branch.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("findByProductIdAndBranchId: returns empty when no record")
    void findByProductIdAndBranchId_noRecord_returnsEmpty() {
        Optional<Inventory> result = inventoryRepository.findByProductIdAndBranchId(productA.getId(), branch.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByBranchId: returns all inventory records for a branch")
    void findByBranchId_returnsAllForBranch() {
        em.persistFlushFind(Inventory.builder().product(productA).branch(branch).quantity(20).build());
        em.persistFlushFind(Inventory.builder().product(productB).branch(branch).quantity(30).build());

        List<Inventory> result = inventoryRepository.findByBranchId(branch.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findLowStockByBranch: returns only items at or below reorder level")
    void findLowStockByBranch_returnsLowItems() {
        em.persistFlushFind(Inventory.builder().product(productA).branch(branch).quantity(5).build());  // reorder=10 → LOW
        em.persistFlushFind(Inventory.builder().product(productB).branch(branch).quantity(20).build()); // reorder=5 → OK

        List<Inventory> lowStock = inventoryRepository.findLowStockByBranch(branch.getId());

        assertThat(lowStock).hasSize(1);
        assertThat(lowStock.get(0).getProduct().getName()).isEqualTo("Product A");
    }

    @Test
    @DisplayName("findAllLowStock: returns all low-stock across all branches")
    void findAllLowStock_returnsAllLowItems() {
        Branch branch2 = em.persistFlushFind(Branch.builder().name("North").address("B").active(true).build());
        em.persistFlushFind(Inventory.builder().product(productA).branch(branch).quantity(3).build());   // LOW
        em.persistFlushFind(Inventory.builder().product(productB).branch(branch2).quantity(2).build());  // LOW
        em.persistFlushFind(Inventory.builder().product(productA).branch(branch2).quantity(100).build()); // OK

        List<Inventory> allLow = inventoryRepository.findAllLowStock();

        assertThat(allLow).hasSize(2);
    }
}
