package zw.co.july28.retail.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zw.co.july28.retail.dto.request.ProductRequest;
import zw.co.july28.retail.dto.response.ProductResponse;
import zw.co.july28.retail.entity.Category;
import zw.co.july28.retail.entity.Product;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.CategoryRepository;
import zw.co.july28.retail.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService unit tests")
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @InjectMocks ProductService productService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(1L).name("Electronics").build();
        product  = Product.builder().id(1L).name("Samsung TV").sku("SAM-TV")
                .barcode("BC001").category(category)
                .costPrice(new BigDecimal("400")).sellingPrice(new BigDecimal("600"))
                .reorderLevel(5).active(true).build();
    }

    @Test
    @DisplayName("getActiveProducts: returns only active products")
    void getActiveProducts_returnsActiveOnly() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getActiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Samsung TV");
    }

    @Test
    @DisplayName("createProduct: duplicate SKU throws BadRequestException")
    void createProduct_duplicateSku_throwsBadRequest() {
        ProductRequest req = new ProductRequest("New Product", "SAM-TV", null, null,
                1L, BigDecimal.TEN, BigDecimal.TEN, 5, true);
        when(productRepository.existsBySku("SAM-TV")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SKU already exists");
    }

    @Test
    @DisplayName("createProduct: valid request saves and returns product")
    void createProduct_valid_savesProduct() {
        ProductRequest req = new ProductRequest("New Phone", "PHONE-001", "BC-PHONE", "Nice phone",
                1L, new BigDecimal("200"), new BigDecimal("350"), 8, true);

        when(productRepository.existsBySku("PHONE-001")).thenReturn(false);
        when(productRepository.existsByBarcode("BC-PHONE")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        ProductResponse result = productService.createProduct(req);

        assertThat(result.getName()).isEqualTo("New Phone");
        assertThat(result.getSellingPrice()).isEqualByComparingTo("350");
    }

    @Test
    @DisplayName("searchProducts: delegates to repository and maps results")
    void searchProducts_delegatesToRepository() {
        when(productRepository.searchProducts("samsung")).thenReturn(List.of(product));

        List<ProductResponse> result = productService.searchProducts("samsung");

        assertThat(result).hasSize(1);
        verify(productRepository).searchProducts("samsung");
    }

    @Test
    @DisplayName("getProductByBarcode: returns product for known barcode")
    void getProductByBarcode_knownBarcode_returnsProduct() {
        when(productRepository.findByBarcode("BC001")).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductByBarcode("BC001");

        assertThat(result.getBarcode()).isEqualTo("BC001");
    }

    @Test
    @DisplayName("getProductByBarcode: throws ResourceNotFoundException for unknown barcode")
    void getProductByBarcode_unknownBarcode_throwsNotFound() {
        when(productRepository.findByBarcode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductByBarcode("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("deleteProduct: deactivates product (soft delete)")
    void deleteProduct_setsActiveToFalse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.deleteProduct(1L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }
}
