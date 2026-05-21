package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.july28.retail.dto.request.StockTransferRequest;
import zw.co.july28.retail.dto.response.StockTransferResponse;
import zw.co.july28.retail.entity.*;
import zw.co.july28.retail.enums.TransferStatus;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.InsufficientStockException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.AppUserRepository;
import zw.co.july28.retail.repository.BranchRepository;
import zw.co.july28.retail.repository.ProductRepository;
import zw.co.july28.retail.repository.StockTransferRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockTransferService {

    private final StockTransferRepository transferRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository userRepository;
    private final InventoryService inventoryService;

    public List<StockTransferResponse> getAllTransfers() {
        return transferRepository.findAll().stream()
                .map(StockTransferResponse::from)
                .collect(Collectors.toList());
    }

    public StockTransferResponse getTransfer(Long id) {
        return StockTransferResponse.from(findById(id));
    }

    public List<StockTransferResponse> getTransfersByBranch(Long branchId) {
        return transferRepository.findByFromBranchIdOrToBranchId(branchId, branchId).stream()
                .map(StockTransferResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public StockTransferResponse createTransfer(StockTransferRequest request, String requesterEmail) {
        if (request.getFromBranchId().equals(request.getToBranchId())) {
            throw new BadRequestException("Source and destination branches must be different");
        }

        Branch fromBranch = branchRepository.findById(request.getFromBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getFromBranchId()));
        Branch toBranch = branchRepository.findById(request.getToBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getToBranchId()));
        AppUser requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterEmail));

        StockTransfer transfer = StockTransfer.builder()
                .fromBranch(fromBranch)
                .toBranch(toBranch)
                .requestedBy(requester)
                .notes(request.getNotes())
                .status(TransferStatus.PENDING)
                .build();

        for (StockTransferRequest.TransferItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));

            StockTransferItem item = StockTransferItem.builder()
                    .stockTransfer(transfer)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .build();
            transfer.getItems().add(item);
        }

        return StockTransferResponse.from(transferRepository.save(transfer));
    }

    @Transactional
    public StockTransferResponse approveTransfer(Long id, String approverEmail) {
        StockTransfer transfer = findById(id);
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new BadRequestException("Only PENDING transfers can be approved");
        }
        AppUser approver = userRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + approverEmail));

        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(approver);
        return StockTransferResponse.from(transferRepository.save(transfer));
    }

    @Transactional
    public StockTransferResponse completeTransfer(Long id) {
        StockTransfer transfer = findById(id);
        if (transfer.getStatus() != TransferStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED transfers can be completed");
        }

        for (StockTransferItem item : transfer.getItems()) {
            int available = inventoryService.getStockLevel(
                    item.getProduct().getId(), transfer.getFromBranch().getId());
            if (available < item.getQuantity()) {
                throw new InsufficientStockException(
                        item.getProduct().getName(), item.getQuantity(), available);
            }
        }

        for (StockTransferItem item : transfer.getItems()) {
            inventoryService.deductStock(item.getProduct(), transfer.getFromBranch(), item.getQuantity());
            inventoryService.addStock(item.getProduct(), transfer.getToBranch(), item.getQuantity());
        }

        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(LocalDateTime.now());
        return StockTransferResponse.from(transferRepository.save(transfer));
    }

    @Transactional
    public StockTransferResponse cancelTransfer(Long id) {
        StockTransfer transfer = findById(id);
        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            throw new BadRequestException("Completed transfers cannot be cancelled");
        }
        transfer.setStatus(TransferStatus.CANCELLED);
        return StockTransferResponse.from(transferRepository.save(transfer));
    }

    private StockTransfer findById(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock Transfer", id));
    }
}
