package zw.co.july28.retail.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zw.co.july28.retail.dto.request.InventoryAdjustRequest;
import zw.co.july28.retail.dto.response.InventoryResponse;
import zw.co.july28.retail.entity.*;
import zw.co.july28.retail.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService unit tests")
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock ProductRepository productRepository;
    @Mock BranchRepository branchRepository;
    @Mock AuditLogService auditLogService;

    InventoryService inventoryService;

    private Branch branch;
    private Product product;

    @BeforeEach
    void setUp() {
        // Manual construction because InventoryService uses @Lazy AuditLogService
        inventoryService = new InventoryService(inventoryRepository, productRepository, branchRepository, auditLogService);

        branch = Branch.builder().id(1L).name("Main").build();
        product = Product.builder().id(1L).name("Coke").sku("COKE")
                .sellingPrice(BigDecimal.ONE).costPrice(BigDecimal.ONE)
                .reorderLevel(10).active(true).build();
    }

    @Test
    @DisplayName("deductStock: reduces existing inventory quantity")
    void deductStock_existingRecord_reducesQuantity() {
        Inventory inv = Inventory.builder().id(1L).product(product).branch(branch).quantity(50).build();
        when(inventoryRepository.findByProductIdAndBranchId(1L, 1L)).thenReturn(Optional.of(inv));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        inventoryService.deductStock(product, branch, 10);

        assertThat(inv.getQuantity()).isEqualTo(40);
        verify(inventoryRepository).save(inv);
    }

    @Test
    @DisplayName("addStock: increases existing inventory quantity")
    void addStock_existingRecord_increasesQuantity() {
        Inventory inv = Inventory.builder().id(1L).product(product).branch(branch).quantity(20).build();
        when(inventoryRepository.findByProductIdAndBranchId(1L, 1L)).thenReturn(Optional.of(inv));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        inventoryService.addStock(product, branch, 15);

        assertThat(inv.getQuantity()).isEqualTo(35);
    }

    @Test
    @DisplayName("getOrCreate: creates zero-quantity record when none exists")
    void getOrCreate_noRecord_createsZeroQuantity() {
        when(inventoryRepository.findByProductIdAndBranchId(1L, 1L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Inventory result = inventoryService.getOrCreate(product, branch);

        assertThat(result.getQuantity()).isZero();
        assertThat(result.getProduct()).isEqualTo(product);
        assertThat(result.getBranch()).isEqualTo(branch);
    }

    @Test
    @DisplayName("getStockLevel: returns quantity from existing record")
    void getStockLevel_existingRecord_returnsQuantity() {
        Inventory inv = Inventory.builder().product(product).branch(branch).quantity(42).build();
        when(inventoryRepository.findByProductIdAndBranchId(1L, 1L)).thenReturn(Optional.of(inv));

        int level = inventoryService.getStockLevel(1L, 1L);

        assertThat(level).isEqualTo(42);
    }

    @Test
    @DisplayName("getStockLevel: returns 0 when no record exists")
    void getStockLevel_noRecord_returnsZero() {
        when(inventoryRepository.findByProductIdAndBranchId(1L, 1L)).thenReturn(Optional.empty());

        int level = inventoryService.getStockLevel(1L, 1L);

        assertThat(level).isZero();
    }

    @Test
    @DisplayName("getLowStockItems: returns items at or below reorder level for a branch")
    void getLowStockItems_branchSpecified_returnsCorrectItems() {
        Inventory lowItem = Inventory.builder().product(product).branch(branch).quantity(5).build();
        when(inventoryRepository.findLowStockByBranch(1L)).thenReturn(List.of(lowItem));

        List<InventoryResponse> result = inventoryService.getLowStockItems(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isLowStock()).isTrue();
    }

    @Test
    @DisplayName("adjustInventory: sets quantity to requested value and logs audit")
    void adjustInventory_setsQuantityAndLogs() {
        InventoryAdjustRequest req = new InventoryAdjustRequest(1L, 1L, 99, "Stocktake");
        Inventory inv = Inventory.builder().product(product).branch(branch).quantity(10).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(inventoryRepository.findByProductIdAndBranchId(1L, 1L)).thenReturn(Optional.of(inv));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InventoryResponse result = inventoryService.adjustInventory(req);

        assertThat(result.getQuantity()).isEqualTo(99);
        verify(auditLogService).log(eq("Inventory"), any(), any(), contains("Stocktake"));
    }
}
