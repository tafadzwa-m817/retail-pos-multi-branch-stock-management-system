package zw.co.july28.retail.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zw.co.july28.retail.entity.TaxRate;
import zw.co.july28.retail.repository.BranchRepository;
import zw.co.july28.retail.repository.TaxRateRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaxRateService unit tests")
class TaxRateServiceTest {

    @Mock TaxRateRepository taxRateRepository;
    @Mock BranchRepository branchRepository;
    @InjectMocks TaxRateService taxRateService;

    @Test
    @DisplayName("calculateTax: 15% VAT on $100 subtotal = $15.00")
    void calculateTax_fifteenPercent_returnsCorrectAmount() {
        TaxRate vat = TaxRate.builder().id(1L).name("VAT 15%").rate(new BigDecimal("15.00")).active(true).build();
        when(taxRateRepository.findApplicableForBranch(1L)).thenReturn(List.of(vat));

        BigDecimal tax = taxRateService.calculateTax(new BigDecimal("100.00"), 1L);

        assertThat(tax).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("calculateTax: stacked rates are combined")
    void calculateTax_stackedRates_combinesCorrectly() {
        TaxRate vat   = TaxRate.builder().id(1L).name("VAT").rate(new BigDecimal("10.00")).active(true).build();
        TaxRate levy  = TaxRate.builder().id(2L).name("Levy").rate(new BigDecimal("5.00")).active(true).build();
        when(taxRateRepository.findApplicableForBranch(1L)).thenReturn(List.of(vat, levy));

        BigDecimal tax = taxRateService.calculateTax(new BigDecimal("200.00"), 1L);

        assertThat(tax).isEqualByComparingTo("30.00"); // 15% × $200
    }

    @Test
    @DisplayName("calculateTax: no rates = $0 tax")
    void calculateTax_noRates_returnsZero() {
        when(taxRateRepository.findApplicableForBranch(1L)).thenReturn(List.of());

        BigDecimal tax = taxRateService.calculateTax(new BigDecimal("100.00"), 1L);

        assertThat(tax).isEqualByComparingTo("0.00");
    }
}
