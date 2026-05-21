package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.july28.retail.entity.Branch;
import zw.co.july28.retail.entity.Product;
import zw.co.july28.retail.entity.Wastage;
import zw.co.july28.retail.enums.AuditAction;
import zw.co.july28.retail.enums.WastageReason;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WastageService {

    private final WastageRepository wastageRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository userRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    public record WastageRequest(Long branchId, Long productId, int quantity,
                                  WastageReason reason, String notes, LocalDate wastedAt) {}

    public List<Wastage> getAll() { return wastageRepository.findAll(); }

    public List<Wastage> getByBranch(Long branchId) { return wastageRepository.findByBranchId(branchId); }

    public BigDecimal getTotalWastageValue(Long branchId, LocalDateTime start, LocalDateTime end) {
        return wastageRepository.sumWastageValueByBranchAndDateRange(branchId, start, end);
    }

    @Transactional
    public Wastage recordWastage(WastageRequest req, String userEmail) {
        Branch branch = branchRepository.findById(req.branchId()).orElseThrow(() -> new ResourceNotFoundException("Branch", req.branchId()));
        Product product = productRepository.findById(req.productId()).orElseThrow(() -> new ResourceNotFoundException("Product", req.productId()));
        var user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Wastage wastage = Wastage.builder()
                .branch(branch).product(product).quantity(req.quantity())
                .reason(req.reason()).notes(req.notes()).recordedBy(user)
                .wastedAt(req.wastedAt() != null ? req.wastedAt().atStartOfDay() : LocalDateTime.now())
                .build();

        inventoryService.deductStock(product, branch, req.quantity());

        Wastage saved = wastageRepository.save(wastage);
        auditLogService.log("Wastage", saved.getId(), AuditAction.CREATE,
                req.quantity() + "x " + product.getName() + " written off at " + branch.getName() + " — " + req.reason());
        return saved;
    }

    public void deleteWastage(Long id) {
        wastageRepository.deleteById(id);
    }
}
