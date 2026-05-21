package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.july28.retail.dto.request.PurchaseOrderRequest;
import zw.co.july28.retail.dto.response.PurchaseOrderResponse;
import zw.co.july28.retail.entity.*;
import zw.co.july28.retail.enums.OrderStatus;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository userRepository;
    private final InventoryService inventoryService;
    private final EmailService emailService;

    public List<PurchaseOrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(PurchaseOrderResponse::from)
                .collect(Collectors.toList());
    }

    public PurchaseOrderResponse getOrder(Long id) {
        return PurchaseOrderResponse.from(findById(id));
    }

    public List<PurchaseOrderResponse> getOrdersByBranch(Long branchId) {
        return orderRepository.findByBranchId(branchId).stream()
                .map(PurchaseOrderResponse::from)
                .collect(Collectors.toList());
    }

    public List<PurchaseOrderResponse> getOrdersBySupplier(Long supplierId) {
        return orderRepository.findBySupplierId(supplierId).stream()
                .map(PurchaseOrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PurchaseOrderResponse createOrder(PurchaseOrderRequest request, String userEmail) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", request.getSupplierId()));
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        PurchaseOrder order = PurchaseOrder.builder()
                .supplier(supplier)
                .branch(branch)
                .orderedBy(user)
                .notes(request.getNotes())
                .status(OrderStatus.DRAFT)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));
            BigDecimal itemTotal = itemReq.getUnitCost().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(itemTotal);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitCost(itemReq.getUnitCost())
                    .totalCost(itemTotal)
                    .build();
            order.getItems().add(item);
        }
        order.setTotalAmount(total);

        return PurchaseOrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public PurchaseOrderResponse submitOrder(Long id) {
        PurchaseOrder order = findById(id);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT orders can be submitted");
        }
        order.setStatus(OrderStatus.ORDERED);
        PurchaseOrder saved = orderRepository.save(order);
        emailService.sendPurchaseOrderEmail(saved);
        return PurchaseOrderResponse.from(saved);
    }

    @Transactional
    public PurchaseOrderResponse receiveOrder(Long id) {
        PurchaseOrder order = findById(id);
        if (order.getStatus() != OrderStatus.ORDERED && order.getStatus() != OrderStatus.PARTIALLY_RECEIVED) {
            throw new BadRequestException("Order must be in ORDERED or PARTIALLY_RECEIVED status to receive");
        }

        for (PurchaseOrderItem item : order.getItems()) {
            inventoryService.addStock(item.getProduct(), order.getBranch(), item.getQuantity());
        }

        order.setStatus(OrderStatus.RECEIVED);
        order.setReceivedAt(LocalDateTime.now());
        return PurchaseOrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public PurchaseOrderResponse cancelOrder(Long id) {
        PurchaseOrder order = findById(id);
        if (order.getStatus() == OrderStatus.RECEIVED) {
            throw new BadRequestException("Received orders cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return PurchaseOrderResponse.from(orderRepository.save(order));
    }

    private PurchaseOrder findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order", id));
    }
}
