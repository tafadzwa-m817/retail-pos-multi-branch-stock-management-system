package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.StockTransfer;
import zw.co.july28.retail.entity.StockTransferItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferResponse {
    private Long id;
    private Long fromBranchId;
    private String fromBranchName;
    private Long toBranchId;
    private String toBranchName;
    private String status;
    private Long requestedById;
    private String requestedByName;
    private Long approvedById;
    private String approvedByName;
    private String notes;
    private List<TransferItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransferItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private int quantity;

        public static TransferItemResponse from(StockTransferItem item) {
            return TransferItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .quantity(item.getQuantity())
                    .build();
        }
    }

    public static StockTransferResponse from(StockTransfer transfer) {
        return StockTransferResponse.builder()
                .id(transfer.getId())
                .fromBranchId(transfer.getFromBranch().getId())
                .fromBranchName(transfer.getFromBranch().getName())
                .toBranchId(transfer.getToBranch().getId())
                .toBranchName(transfer.getToBranch().getName())
                .status(transfer.getStatus().name())
                .requestedById(transfer.getRequestedBy() != null ? transfer.getRequestedBy().getId() : null)
                .requestedByName(transfer.getRequestedBy() != null
                        ? transfer.getRequestedBy().getFirstName() + " " + transfer.getRequestedBy().getLastName() : null)
                .approvedById(transfer.getApprovedBy() != null ? transfer.getApprovedBy().getId() : null)
                .approvedByName(transfer.getApprovedBy() != null
                        ? transfer.getApprovedBy().getFirstName() + " " + transfer.getApprovedBy().getLastName() : null)
                .notes(transfer.getNotes())
                .items(transfer.getItems().stream().map(TransferItemResponse::from).collect(Collectors.toList()))
                .createdAt(transfer.getCreatedAt())
                .completedAt(transfer.getCompletedAt())
                .build();
    }
}
