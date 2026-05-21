package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.july28.retail.dto.request.SaleRequest;
import zw.co.july28.retail.dto.response.PageResponse;
import zw.co.july28.retail.dto.response.SaleResponse;
import zw.co.july28.retail.entity.*;
import zw.co.july28.retail.enums.AuditAction;
import zw.co.july28.retail.enums.DiscountType;
import zw.co.july28.retail.enums.SaleStatus;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.InsufficientStockException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.*;
import zw.co.july28.retail.security.SecurityContextHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private final SaleRepository saleRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final CustomerService customerService;
    private final PromotionService promotionService;
    private final AuditLogService auditLogService;
    private final TaxRateService taxRateService;
    private final EmailService emailService;
    private final SecurityContextHelper securityContextHelper;

    @Value("${admin.alert-email:admin@july28retail.co.zw}")
    private String adminAlertEmail;

    // ── Role-based list — branch staff only see their branch ──────────────────

    public PageResponse<SaleResponse> getAllSales(Pageable pageable) {
        var branchId = securityContextHelper.getCurrentUserBranchId();
        if (branchId.isPresent()) {
            return PageResponse.from(saleRepository.findByBranchId(branchId.get(), pageable).map(SaleResponse::from));
        }
        return PageResponse.from(saleRepository.findAll(pageable).map(SaleResponse::from));
    }

    public List<SaleResponse> getAllSales() {
        var branchId = securityContextHelper.getCurrentUserBranchId();
        if (branchId.isPresent()) {
            return saleRepository.findByBranchId(branchId.get()).stream().map(SaleResponse::from).collect(Collectors.toList());
        }
        return saleRepository.findAll().stream().map(SaleResponse::from).collect(Collectors.toList());
    }

    public SaleResponse getSale(Long id) {
        return SaleResponse.from(findById(id));
    }

    public List<SaleResponse> getSalesByBranch(Long branchId) {
        return saleRepository.findByBranchId(branchId).stream()
                .map(SaleResponse::from).collect(Collectors.toList());
    }

    public List<SaleResponse> getSalesByDateRange(Long branchId, LocalDateTime start, LocalDateTime end) {
        // If branch-scoped user, force their branch
        var userBranch = securityContextHelper.getCurrentUserBranchId();
        Long effectiveBranchId = userBranch.orElse(branchId);
        List<Sale> sales = effectiveBranchId != null
                ? saleRepository.findByBranchAndDateRange(effectiveBranchId, start, end)
                : saleRepository.findByDateRange(start, end);
        return sales.stream().map(SaleResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public SaleResponse createSale(SaleRequest request, String cashierEmail) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
        AppUser cashier = userRepository.findByEmail(cashierEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + cashierEmail));

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));
        }

        // ── Loyalty redemption validation ─────────────────────────────────────
        int pointsToRedeem = request.getLoyaltyPointsToRedeem();
        if (pointsToRedeem > 0) {
            if (customer == null) throw new BadRequestException("A customer must be selected to redeem loyalty points");
            if (customer.getLoyaltyPoints() < pointsToRedeem) {
                throw new BadRequestException("Customer only has " + customer.getLoyaltyPoints() + " loyalty points (requested " + pointsToRedeem + ")");
            }
        }

        List<Promotion> activePromotions = promotionService.findCurrentlyActivePromotions();

        Sale sale = Sale.builder()
                .branch(branch).cashier(cashier).customer(customer)
                .paymentMethod(request.getPaymentMethod())
                .paymentReference(request.getPaymentReference())
                .notes(request.getNotes())
                .status(SaleStatus.COMPLETED)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (SaleRequest.SaleItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));

            int stockLevel = inventoryService.getStockLevel(product.getId(), branch.getId());
            if (stockLevel < itemReq.getQuantity()) {
                throw new InsufficientStockException(product.getName(), itemReq.getQuantity(), stockLevel);
            }

            BigDecimal lineSubtotal = product.getSellingPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal manualDiscount = itemReq.getDiscountAmount() != null ? itemReq.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal promotionDiscount = calculatePromotionDiscount(product, lineSubtotal, activePromotions);
            BigDecimal totalItemDiscount = manualDiscount.add(promotionDiscount);

            sale.getItems().add(SaleItem.builder()
                    .sale(sale).product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getSellingPrice())
                    .discountAmount(totalItemDiscount)
                    .totalPrice(lineSubtotal.subtract(totalItemDiscount))
                    .build());

            subtotal = subtotal.add(lineSubtotal);
            totalDiscount = totalDiscount.add(totalItemDiscount);
        }

        // ── Apply loyalty discount ($0.01 per point) ──────────────────────────
        BigDecimal loyaltyDiscount = BigDecimal.ZERO;
        if (pointsToRedeem > 0) {
            loyaltyDiscount = BigDecimal.valueOf(pointsToRedeem).multiply(new BigDecimal("0.01"));
            totalDiscount = totalDiscount.add(loyaltyDiscount);
        }

        BigDecimal afterDiscount = subtotal.subtract(totalDiscount).max(BigDecimal.ZERO);
        BigDecimal taxAmount = taxRateService.calculateTax(afterDiscount, branch.getId());

        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(totalDiscount);
        sale.setTaxAmount(taxAmount);
        sale.setTotalAmount(afterDiscount.add(taxAmount));

        Sale saved = saleRepository.save(sale);

        // ── Deduct inventory + collect low-stock alerts ───────────────────────
        List<Inventory> nowLowStock = new ArrayList<>();
        for (SaleRequest.SaleItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId()).orElseThrow();
            inventoryService.deductStock(product, branch, itemReq.getQuantity());
            int newLevel = inventoryService.getStockLevel(product.getId(), branch.getId());
            if (newLevel <= product.getReorderLevel()) {
                inventoryRepository.findByProductIdAndBranchId(product.getId(), branch.getId())
                        .ifPresent(nowLowStock::add);
            }
        }

        // ── Loyalty points — deduct redeemed, award earned ────────────────────
        if (customer != null) {
            if (pointsToRedeem > 0) {
                customerService.deductLoyaltyPoints(customer, pointsToRedeem);
            }
            int earned = saved.getTotalAmount().intValue();
            if (earned > 0) {
                customerService.addLoyaltyPoints(customer, earned);
            }
        }

        auditLogService.log("Sale", saved.getId(), AuditAction.CREATE,
                "Sale at " + branch.getName() + " — total: $" + saved.getTotalAmount()
                + (pointsToRedeem > 0 ? " | redeemed " + pointsToRedeem + " pts" : ""));

        emailService.sendReceiptEmail(saved);

        // ── Stock alert email ─────────────────────────────────────────────────
        if (!nowLowStock.isEmpty()) {
            emailService.sendLowStockAlert(nowLowStock, adminAlertEmail);
            log.info("Low-stock alert triggered for {} items after sale #{}", nowLowStock.size(), saved.getId());
        }

        return SaleResponse.from(saved);
    }

    @Transactional
    public SaleResponse voidSale(Long id) {
        Sale sale = findById(id);
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new BadRequestException("Only COMPLETED sales can be voided");
        }
        for (SaleItem item : sale.getItems()) {
            inventoryService.addStock(item.getProduct(), sale.getBranch(), item.getQuantity());
        }
        sale.setStatus(SaleStatus.VOIDED);
        Sale saved = saleRepository.save(sale);
        auditLogService.log("Sale", id, AuditAction.VOID, "Sale voided at " + sale.getBranch().getName());
        return SaleResponse.from(saved);
    }

    private BigDecimal calculatePromotionDiscount(Product product, BigDecimal lineSubtotal, List<Promotion> promotions) {
        BigDecimal total = BigDecimal.ZERO;
        for (Promotion promo : promotions) {
            boolean applies = promo.isApplyToAll() ||
                    promo.getApplicableProducts().stream().anyMatch(p -> p.getId().equals(product.getId()));
            if (!applies) continue;
            if (promo.getMinimumPurchaseAmount() != null && lineSubtotal.compareTo(promo.getMinimumPurchaseAmount()) < 0) continue;

            BigDecimal discount = promo.getDiscountType() == DiscountType.PERCENTAGE
                    ? lineSubtotal.multiply(promo.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : promo.getDiscountValue().min(lineSubtotal);
            total = total.add(discount);
        }
        return total;
    }

    private Sale findById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }
}
