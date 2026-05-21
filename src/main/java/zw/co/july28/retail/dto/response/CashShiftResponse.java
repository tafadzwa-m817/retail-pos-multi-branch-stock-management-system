package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.CashShift;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashShiftResponse {
    private Long id;
    private Long branchId;
    private String branchName;
    private Long openedById;
    private String openedByName;
    private Long closedById;
    private String closedByName;
    private BigDecimal openingFloat;
    private BigDecimal closingCash;
    private BigDecimal totalSalesAmount;
    private int totalTransactions;
    private BigDecimal variance;
    private String status;
    private String notes;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    public static CashShiftResponse from(CashShift shift) {
        BigDecimal closingCash = shift.getClosingCash();
        BigDecimal openingFloat = shift.getOpeningFloat();
        BigDecimal totalSales = shift.getTotalSalesAmount();
        BigDecimal variance = (closingCash != null)
                ? closingCash.subtract(openingFloat).subtract(totalSales)
                : null;

        return CashShiftResponse.builder()
                .id(shift.getId())
                .branchId(shift.getBranch().getId())
                .branchName(shift.getBranch().getName())
                .openedById(shift.getOpenedBy().getId())
                .openedByName(shift.getOpenedBy().getFirstName() + " " + shift.getOpenedBy().getLastName())
                .closedById(shift.getClosedBy() != null ? shift.getClosedBy().getId() : null)
                .closedByName(shift.getClosedBy() != null
                        ? shift.getClosedBy().getFirstName() + " " + shift.getClosedBy().getLastName() : null)
                .openingFloat(openingFloat)
                .closingCash(closingCash)
                .totalSalesAmount(totalSales)
                .totalTransactions(shift.getTotalTransactions())
                .variance(variance)
                .status(shift.getStatus().name())
                .notes(shift.getNotes())
                .openedAt(shift.getOpenedAt())
                .closedAt(shift.getClosedAt())
                .build();
    }
}
