package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.entity.Branch;
import zw.co.july28.retail.entity.TaxRate;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.BranchRepository;
import zw.co.july28.retail.repository.TaxRateRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxRateService {

    private final TaxRateRepository taxRateRepository;
    private final BranchRepository branchRepository;

    public List<TaxRate> getAll() {
        return taxRateRepository.findAll();
    }

    public List<TaxRate> getActive() {
        return taxRateRepository.findByActiveTrue();
    }

    public List<TaxRate> getForBranch(Long branchId) {
        return taxRateRepository.findApplicableForBranch(branchId);
    }

    public TaxRate create(String name, BigDecimal rate, Long branchId) {
        Branch branch = branchId != null
                ? branchRepository.findById(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch", branchId))
                : null;
        return taxRateRepository.save(TaxRate.builder().name(name).rate(rate).branch(branch).active(true).build());
    }

    public TaxRate update(Long id, String name, BigDecimal rate, boolean active) {
        TaxRate taxRate = taxRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaxRate", id));
        taxRate.setName(name);
        taxRate.setRate(rate);
        taxRate.setActive(active);
        return taxRateRepository.save(taxRate);
    }

    public void delete(Long id) {
        taxRateRepository.deleteById(id);
    }

    /** Calculates combined tax amount for a given subtotal and branch */
    public BigDecimal calculateTax(BigDecimal subtotal, Long branchId) {
        List<TaxRate> rates = taxRateRepository.findApplicableForBranch(branchId);
        BigDecimal totalRate = rates.stream()
                .map(TaxRate::getRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return subtotal.multiply(totalRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
