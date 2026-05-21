package zw.co.july28.retail.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import zw.co.july28.retail.entity.*;
import zw.co.july28.retail.enums.PaymentMethod;
import zw.co.july28.retail.enums.Role;
import zw.co.july28.retail.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("SaleRepository @DataJpaTest")
class SaleRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired SaleRepository saleRepository;

    private Branch branch;
    private AppUser cashier;

    @BeforeEach
    void setUp() {
        branch = em.persistFlushFind(Branch.builder().name("Main").address("HRE").active(true).build());
        cashier = em.persistFlushFind(AppUser.builder()
                .firstName("C").lastName("C").email("cashier@t.com").password("pw")
                .role(Role.CASHIER).branch(branch).active(true).build());
    }

    private Sale savedSale(BigDecimal total, LocalDateTime createdAt) {
        Sale sale = Sale.builder()
                .branch(branch).cashier(cashier)
                .subtotal(total).discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO).totalAmount(total)
                .paymentMethod(PaymentMethod.CASH)
                .status(SaleStatus.COMPLETED)
                .build();
        sale = em.persist(sale);
        // Manually set createdAt since @PrePersist sets it on persist
        em.getEntityManager().createQuery("UPDATE Sale s SET s.createdAt = :dt WHERE s.id = :id")
                .setParameter("dt", createdAt).setParameter("id", sale.getId()).executeUpdate();
        em.flush();
        return sale;
    }

    @Test
    @DisplayName("findByBranchId: returns all sales for a branch")
    void findByBranchId_returnsCorrectSales() {
        savedSale(new BigDecimal("50.00"), LocalDateTime.now());
        savedSale(new BigDecimal("75.00"), LocalDateTime.now());

        List<Sale> result = saleRepository.findByBranchId(branch.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByDateRange: returns sales within specified date range")
    void findByDateRange_returnsCorrectSales() {
        LocalDateTime now = LocalDateTime.now();
        savedSale(new BigDecimal("100.00"), now.minusDays(1));
        savedSale(new BigDecimal("200.00"), now.minusDays(10)); // outside range

        List<Sale> result = saleRepository.findByDateRange(now.minusDays(3), now);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("findByBranchAndDateRange: filters by both branch and date range")
    void findByBranchAndDateRange_filtersCorrectly() {
        Branch otherBranch = em.persistFlushFind(Branch.builder().name("North").address("B").active(true).build());
        AppUser otherCashier = em.persistFlushFind(AppUser.builder()
                .firstName("O").lastName("C").email("other@t.com").password("pw")
                .role(Role.CASHIER).branch(otherBranch).active(true).build());

        LocalDateTime now = LocalDateTime.now();
        savedSale(new BigDecimal("50.00"), now.minusDays(1)); // in range, correct branch
        // sale on other branch
        Sale otherSale = Sale.builder().branch(otherBranch).cashier(otherCashier)
                .subtotal(new BigDecimal("100")).discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("100"))
                .paymentMethod(PaymentMethod.CASH).status(SaleStatus.COMPLETED).build();
        em.persist(otherSale);
        em.flush();

        List<Sale> result = saleRepository.findByBranchAndDateRange(branch.getId(), now.minusDays(2), now);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBranch().getName()).isEqualTo("Main");
    }
}
