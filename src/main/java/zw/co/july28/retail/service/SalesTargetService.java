package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.entity.AppUser;
import zw.co.july28.retail.entity.Branch;
import zw.co.july28.retail.entity.SalesTarget;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesTargetService {

    private final SalesTargetRepository targetRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final SaleRepository saleRepository;

    public record TargetProgress(Long branchId, String branchName, BigDecimal target,
                                  BigDecimal achieved, BigDecimal progressPct, int month, int year) {}

    public SalesTarget setTarget(Long branchId, BigDecimal amount, int month, int year, String userEmail) {
        if (month < 1 || month > 12) throw new BadRequestException("Month must be 1–12");
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        AppUser user = userRepository.findByEmail(userEmail).orElseThrow();

        SalesTarget target = targetRepository.findByBranchIdAndMonthAndYear(branchId, month, year)
                .orElse(SalesTarget.builder().branch(branch).month(month).year(year).createdBy(user).build());
        target.setTargetAmount(amount);
        return targetRepository.save(target);
    }

    public List<TargetProgress> getCurrentMonthProgress() {
        int month = LocalDate.now().getMonthValue();
        int year  = LocalDate.now().getYear();
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end   = LocalDateTime.now();

        List<SalesTarget> targets = targetRepository.findByMonthAndYear(month, year);

        return targets.stream().map(t -> {
            BigDecimal achieved = saleRepository.sumRevenueByBranchAndDateRange(t.getBranch().getId(), start, end);
            if (achieved == null) achieved = BigDecimal.ZERO;
            BigDecimal pct = t.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                    ? achieved.multiply(BigDecimal.valueOf(100)).divide(t.getTargetAmount(), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return new TargetProgress(t.getBranch().getId(), t.getBranch().getName(),
                    t.getTargetAmount(), achieved, pct, month, year);
        }).collect(Collectors.toList());
    }

    public List<SalesTarget> getAllTargets() { return targetRepository.findAll(); }

    public void deleteTarget(Long id) {
        if (!targetRepository.existsById(id)) throw new ResourceNotFoundException("SalesTarget", id);
        targetRepository.deleteById(id);
    }
}
