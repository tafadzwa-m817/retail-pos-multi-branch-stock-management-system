package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.july28.retail.dto.request.CloseShiftRequest;
import zw.co.july28.retail.dto.request.OpenShiftRequest;
import zw.co.july28.retail.dto.response.CashShiftResponse;
import zw.co.july28.retail.entity.AppUser;
import zw.co.july28.retail.entity.Branch;
import zw.co.july28.retail.entity.CashShift;
import zw.co.july28.retail.entity.Sale;
import zw.co.july28.retail.enums.ShiftStatus;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.AppUserRepository;
import zw.co.july28.retail.repository.BranchRepository;
import zw.co.july28.retail.repository.CashShiftRepository;
import zw.co.july28.retail.repository.SaleRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashShiftService {

    private final CashShiftRepository cashShiftRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final SaleRepository saleRepository;

    public List<CashShiftResponse> getShiftsByBranch(Long branchId) {
        return cashShiftRepository.findByBranchIdOrderByOpenedAtDesc(branchId).stream()
                .map(CashShiftResponse::from)
                .collect(Collectors.toList());
    }

    public CashShiftResponse getShift(Long id) {
        return CashShiftResponse.from(findById(id));
    }

    public CashShiftResponse getCurrentShift(Long branchId) {
        return cashShiftRepository.findByBranchIdAndStatus(branchId, ShiftStatus.OPEN)
                .map(CashShiftResponse::from)
                .orElseThrow(() -> new BadRequestException("No open shift found for this branch"));
    }

    @Transactional
    public CashShiftResponse openShift(OpenShiftRequest request, String userEmail) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));

        if (cashShiftRepository.existsByBranchIdAndStatus(request.getBranchId(), ShiftStatus.OPEN)) {
            throw new BadRequestException("Branch already has an open shift. Close it before opening a new one.");
        }

        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        CashShift shift = CashShift.builder()
                .branch(branch)
                .openedBy(user)
                .openingFloat(request.getOpeningFloat())
                .status(ShiftStatus.OPEN)
                .notes(request.getNotes())
                .build();

        return CashShiftResponse.from(cashShiftRepository.save(shift));
    }

    @Transactional
    public CashShiftResponse closeShift(Long shiftId, CloseShiftRequest request, String userEmail) {
        CashShift shift = findById(shiftId);

        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new BadRequestException("Shift is already closed");
        }

        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        LocalDateTime closedAt = LocalDateTime.now();

        // Aggregate sales during this shift period
        List<Sale> sales = saleRepository.findByBranchAndDateRange(
                shift.getBranch().getId(), shift.getOpenedAt(), closedAt);

        BigDecimal totalSales = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        shift.setClosedBy(user);
        shift.setClosingCash(request.getClosingCash());
        shift.setTotalSalesAmount(totalSales);
        shift.setTotalTransactions(sales.size());
        shift.setStatus(ShiftStatus.CLOSED);
        shift.setClosedAt(closedAt);
        if (request.getNotes() != null) {
            shift.setNotes(request.getNotes());
        }

        return CashShiftResponse.from(cashShiftRepository.save(shift));
    }

    private CashShift findById(Long id) {
        return cashShiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cash Shift", id));
    }
}
