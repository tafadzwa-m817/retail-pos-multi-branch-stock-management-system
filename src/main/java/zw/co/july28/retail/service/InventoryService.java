package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.july28.retail.dto.request.InventoryAdjustRequest;
import zw.co.july28.retail.enums.AuditAction;
import zw.co.july28.retail.dto.response.InventoryResponse;
import zw.co.july28.retail.entity.Branch;
import zw.co.july28.retail.entity.Inventory;
import zw.co.july28.retail.entity.Product;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.BranchRepository;
import zw.co.july28.retail.repository.InventoryRepository;
import zw.co.july28.retail.repository.ProductRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;

    public InventoryService(InventoryRepository inventoryRepository,
                            ProductRepository productRepository,
                            BranchRepository branchRepository,
                            @Lazy AuditLogService auditLogService) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.auditLogService = auditLogService;
    }

    public List<InventoryResponse> getInventoryByBranch(Long branchId) {
        return inventoryRepository.findByBranchId(branchId).stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }

    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }

    public List<InventoryResponse> getLowStockItems(Long branchId) {
        List<Inventory> items = branchId != null
                ? inventoryRepository.findLowStockByBranch(branchId)
                : inventoryRepository.findAllLowStock();
        return items.stream().map(InventoryResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public InventoryResponse adjustInventory(InventoryAdjustRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));

        Inventory inventory = inventoryRepository
                .findByProductIdAndBranchId(request.getProductId(), request.getBranchId())
                .orElse(Inventory.builder().product(product).branch(branch).quantity(0).build());

        inventory.setQuantity(request.getQuantity());
        InventoryResponse response = InventoryResponse.from(inventoryRepository.save(inventory));
        auditLogService.log("Inventory", response.getId(), AuditAction.ADJUST,
                "Stock adjusted for " + product.getName() + " at " + branch.getName() +
                " → qty: " + request.getQuantity() +
                (request.getReason() != null ? " | reason: " + request.getReason() : ""));
        return response;
    }

    @Transactional
    public Inventory getOrCreate(Product product, Branch branch) {
        return inventoryRepository
                .findByProductIdAndBranchId(product.getId(), branch.getId())
                .orElseGet(() -> inventoryRepository.save(
                        Inventory.builder().product(product).branch(branch).quantity(0).build()
                ));
    }

    @Transactional
    public void deductStock(Product product, Branch branch, int quantity) {
        Inventory inventory = getOrCreate(product, branch);
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);
    }

    @Transactional
    public void addStock(Product product, Branch branch, int quantity) {
        Inventory inventory = getOrCreate(product, branch);
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);
    }

    public int getStockLevel(Long productId, Long branchId) {
        return inventoryRepository.findByProductIdAndBranchId(productId, branchId)
                .map(Inventory::getQuantity)
                .orElse(0);
    }

    /**
     * Returns all inventory records below their reorder level.
     * Used for the reorder-suggestion feature.
     */
    public List<Inventory> getAllLowStockRaw() {
        return inventoryRepository.findAllLowStock();
    }
}
