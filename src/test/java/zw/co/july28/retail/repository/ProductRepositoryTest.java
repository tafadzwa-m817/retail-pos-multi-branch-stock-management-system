package zw.co.july28.retail.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import zw.co.july28.retail.entity.Category;
import zw.co.july28.retail.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ProductRepository @DataJpaTest")
class ProductRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ProductRepository productRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        category = em.persistFlushFind(Category.builder().name("TestCat").description("desc").build());
    }

    private Product savedProduct(String name, String sku, String barcode, boolean active) {
        return em.persistFlushFind(Product.builder()
                .name(name).sku(sku).barcode(barcode).category(category)
                .costPrice(BigDecimal.ONE).sellingPrice(BigDecimal.TEN)
                .reorderLevel(5).active(active).build());
    }

    @Test
    @DisplayName("findByActiveTrue: returns only active products")
    void findByActiveTrue_returnsOnlyActive() {
        savedProduct("Active Product", "ACT-001", "BC001", true);
        savedProduct("Inactive Product", "INA-001", "BC002", false);

        List<Product> result = productRepository.findByActiveTrue();

        assertThat(result).allMatch(Product::isActive);
        assertThat(result.stream().map(Product::getSku)).contains("ACT-001")
                .doesNotContain("INA-001");
    }

    @Test
    @DisplayName("findBySku: returns product for exact SKU match")
    void findBySku_exactMatch_returnsProduct() {
        savedProduct("Phone", "PHONE-001", "BC-P001", true);

        Optional<Product> result = productRepository.findBySku("PHONE-001");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Phone");
    }

    @Test
    @DisplayName("findByBarcode: returns product for exact barcode match")
    void findByBarcode_exactMatch_returnsProduct() {
        savedProduct("Tablet", "TAB-001", "BARCODE-TAB", true);

        Optional<Product> result = productRepository.findByBarcode("BARCODE-TAB");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Tablet");
    }

    @Test
    @DisplayName("searchProducts: finds by partial name match (case-insensitive)")
    void searchProducts_partialName_returnsMatches() {
        savedProduct("Samsung Galaxy", "SAM-001", "BC-SAM", true);
        savedProduct("Apple iPhone", "APL-001", "BC-APL", true);

        List<Product> result = productRepository.searchProducts("samsung");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Samsung Galaxy");
    }

    @Test
    @DisplayName("searchProducts: finds by partial SKU match")
    void searchProducts_partialSku_returnsMatches() {
        savedProduct("Sony TV", "SONY-TV-55", "BC-SONY", true);

        List<Product> result = productRepository.searchProducts("SONY");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("searchProducts: finds by barcode match")
    void searchProducts_barcode_returnsMatch() {
        savedProduct("Coke 500ml", "COKE-500", "6001234567890", true);

        List<Product> result = productRepository.searchProducts("6001234567890");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("COKE-500");
    }

    @Test
    @DisplayName("existsBySku: returns true for existing SKU")
    void existsBySku_existingSku_returnsTrue() {
        savedProduct("Laptop", "LAP-001", "BC-LAP", true);

        assertThat(productRepository.existsBySku("LAP-001")).isTrue();
        assertThat(productRepository.existsBySku("NOT-EXISTS")).isFalse();
    }
}
