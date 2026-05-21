package zw.co.july28.retail.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zw.co.july28.retail.dto.request.StockTransferRequest;
import zw.co.july28.retail.dto.response.StockTransferResponse;
import zw.co.july28.retail.entity.*;
import zw.co.july28.retail.enums.TransferStatus;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.InsufficientStockException;
import zw.co.july28.retail.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockTransferService unit tests")
class StockTransferServiceTest {

    @Mock StockTransferRepository transferRepository;
    @Mock BranchRepository branchRepository;
    @Mock ProductRepository productRepository;
    @Mock AppUserRepository userRepository;
    @Mock InventoryService inventoryService;

    @InjectMocks StockTransferService transferService;

    private Branch fromBranch;
    private Branch toBranch;
    private AppUser requester;
    private Product product;

    @BeforeEach
    void setUp() {
        fromBranch = Branch.builder().id(1L).name("Main Branch").build();
        toBranch   = Branch.builder().id(2L).name("North Branch").build();
        requester  = AppUser.builder().id(1L).email("manager@test.com").firstName("Test").lastName("Manager").build();
        product    = Product.builder().id(1L).name("Coke").sku("COKE")
                .sellingPrice(BigDecimal.ONE).costPrice(BigDecimal.ONE)
                .reorderLevel(5).active(true).build();
    }

    @Test
    @DisplayName("createTransfer: valid request creates PENDING transfer")
    void createTransfer_valid_createsPendingTransfer() {
        StockTransferRequest req = new StockTransferRequest(1L, 2L, "Test transfer",
                List.of(new StockTransferRequest.TransferItemRequest(1L, 10)));

        when(branchRepository.findById(1L)).thenReturn(Optional.of(fromBranch));
        when(branchRepository.findById(2L)).thenReturn(Optional.of(toBranch));
        when(userRepository.findByEmail("manager@test.com")).thenReturn(Optional.of(requester));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(transferRepository.save(any())).thenAnswer(inv -> {
            StockTransfer t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        StockTransferResponse result = transferService.createTransfer(req, "manager@test.com");

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getFromBranchName()).isEqualTo("Main Branch");
    }

    @Test
    @DisplayName("createTransfer: same source and destination throws BadRequestException")
    void createTransfer_sameBranches_throwsBadRequest() {
        StockTransferRequest req = new StockTransferRequest(1L, 1L, null,
                List.of(new StockTransferRequest.TransferItemRequest(1L, 5)));

        assertThatThrownBy(() -> transferService.createTransfer(req, "manager@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be different");
    }

    @Test
    @DisplayName("approveTransfer: PENDING transfer transitions to APPROVED")
    void approveTransfer_pending_becomesApproved() {
        StockTransfer transfer = StockTransfer.builder().id(1L)
                .fromBranch(fromBranch).toBranch(toBranch)
                .status(TransferStatus.PENDING).items(new ArrayList<>()).build();

        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(userRepository.findByEmail("manager@test.com")).thenReturn(Optional.of(requester));
        when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StockTransferResponse result = transferService.approveTransfer(1L, "manager@test.com");

        assertThat(result.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("approveTransfer: non-PENDING transfer throws BadRequestException")
    void approveTransfer_nonPending_throwsBadRequest() {
        StockTransfer transfer = StockTransfer.builder().id(1L)
                .fromBranch(fromBranch).toBranch(toBranch)
                .status(TransferStatus.APPROVED).items(new ArrayList<>()).build();

        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> transferService.approveTransfer(1L, "manager@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("completeTransfer: APPROVED transfer moves stock between branches")
    void completeTransfer_approved_movesStock() {
        StockTransferItem item = StockTransferItem.builder().product(product).quantity(5).build();
        StockTransfer transfer = StockTransfer.builder().id(1L)
                .fromBranch(fromBranch).toBranch(toBranch)
                .status(TransferStatus.APPROVED)
                .items(List.of(item)).build();

        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(inventoryService.getStockLevel(1L, 1L)).thenReturn(20);
        when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StockTransferResponse result = transferService.completeTransfer(1L);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(inventoryService).deductStock(product, fromBranch, 5);
        verify(inventoryService).addStock(product, toBranch, 5);
    }

    @Test
    @DisplayName("completeTransfer: insufficient source stock throws InsufficientStockException")
    void completeTransfer_insufficientStock_throwsException() {
        StockTransferItem item = StockTransferItem.builder().product(product).quantity(100).build();
        StockTransfer transfer = StockTransfer.builder().id(1L)
                .fromBranch(fromBranch).toBranch(toBranch)
                .status(TransferStatus.APPROVED)
                .items(List.of(item)).build();

        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(inventoryService.getStockLevel(1L, 1L)).thenReturn(5);

        assertThatThrownBy(() -> transferService.completeTransfer(1L))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Coke");
    }

    @Test
    @DisplayName("cancelTransfer: COMPLETED transfer cannot be cancelled")
    void cancelTransfer_completed_throwsBadRequest() {
        StockTransfer transfer = StockTransfer.builder().id(1L)
                .fromBranch(fromBranch).toBranch(toBranch)
                .status(TransferStatus.COMPLETED).items(new ArrayList<>()).build();

        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> transferService.cancelTransfer(1L))
                .isInstanceOf(BadRequestException.class);
    }
}
