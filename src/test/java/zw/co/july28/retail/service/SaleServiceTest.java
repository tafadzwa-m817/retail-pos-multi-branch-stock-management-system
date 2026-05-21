package zw.co.july28.retail.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zw.co.july28.retail.dto.request.SaleRequest;
import zw.co.july28.retail.dto.response.SaleResponse;
import zw.co.july28.retail.entity.*;
import zw.co.july28.retail.enums.PaymentMethod;
import zw.co.july28.retail.enums.Role;
import zw.co.july28.retail.enums.SaleStatus;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.InsufficientStockException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.*;
import zw.co.july28.retail.security.SecurityContextHelper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaleService unit tests")
class SaleServiceTest {

    @Mock SaleRepository saleRepository;
    @Mock BranchRepository branchRepository;
    @Mock AppUserRepository userRepository;
    @Mock CustomerRepository customerRepository;
    @Mock ProductRepository productRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock InventoryService inventoryService;
    @Mock CustomerService customerService;
    @Mock PromotionService promotionService;
    @Mock AuditLogService auditLogService;
    @Mock TaxRateService taxRateService;
    @Mock EmailService emailService;
    @Mock SecurityContextHelper securityContextHelper;

    @InjectMocks SaleService saleService;

    private Branch branch;
    private AppUser cashier;
    private Product product;

    @BeforeEach
    void setUp() {
        branch = Branch.builder().id(1L).name("Main Branch").active(true).build();
        cashier = AppUser.builder().id(1L).firstName("Test").lastName("Cashier")
                .email("cashier@test.com").role(Role.CASHIER).active(true).build();
        product = Product.builder().id(1L).name("Coca-Cola").sku("COKE")
                .sellingPrice(new BigDecimal("1.00")).costPrice(new BigDecimal("0.60"))
                .reorderLevel(10).active(true).build();
    }

    @Test
    @DisplayName("createSale: valid request creates sale and deducts inventory once")
    void createSale_validRequest_createsSaleAndDeductsInventory() {
        SaleRequest req = new SaleRequest(1L, null, PaymentMethod.CASH, null,
                List.of(new SaleRequest.SaleItemRequest(1L, 2, BigDecimal.ZERO)), null, 0);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findByEmail("cashier@test.com")).thenReturn(Optional.of(cashier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryService.getStockLevel(1L, 1L)).thenReturn(50);
        when(promotionService.findCurrentlyActivePromotions()).thenReturn(List.of());
        when(taxRateService.calculateTax(any(), any())).thenReturn(BigDecimal.ZERO);
        when(saleRepository.save(any())).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        SaleResponse result = saleService.createSale(req, "cashier@test.com");

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo("2.00");
        verify(inventoryService, times(1)).deductStock(any(), any(), eq(2));
        verify(auditLogService).log(eq("Sale"), anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("createSale: insufficient stock throws InsufficientStockException")
    void createSale_insufficientStock_throwsException() {
        SaleRequest req = new SaleRequest(1L, null, PaymentMethod.CASH, null,
                List.of(new SaleRequest.SaleItemRequest(1L, 100, BigDecimal.ZERO)), null, 0);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findByEmail("cashier@test.com")).thenReturn(Optional.of(cashier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryService.getStockLevel(1L, 1L)).thenReturn(5);
        when(promotionService.findCurrentlyActivePromotions()).thenReturn(List.of());

        assertThatThrownBy(() -> saleService.createSale(req, "cashier@test.com"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Coca-Cola");

        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSale: loyalty points redemption reduces total and deducts points")
    void createSale_loyaltyRedemption_reducesTotalAndDeductsPoints() {
        Customer customer = Customer.builder().id(5L).firstName("John").lastName("Doe")
                .loyaltyPoints(500).build();

        SaleRequest req = new SaleRequest(1L, 5L, PaymentMethod.CASH, null,
                List.of(new SaleRequest.SaleItemRequest(1L, 2, BigDecimal.ZERO)), null, 100);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findByEmail("cashier@test.com")).thenReturn(Optional.of(cashier));
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryService.getStockLevel(1L, 1L)).thenReturn(50);
        when(promotionService.findCurrentlyActivePromotions()).thenReturn(List.of());
        when(taxRateService.calculateTax(any(), any())).thenReturn(BigDecimal.ZERO);
        when(saleRepository.save(any())).thenAnswer(inv -> { Sale s = inv.getArgument(0); s.setId(1L); return s; });

        SaleResponse result = saleService.createSale(req, "cashier@test.com");

        // 2 × $1.00 = $2.00 − $1.00 loyalty (100 pts × $0.01) = $1.00 total
        assertThat(result.getTotalAmount()).isEqualByComparingTo("1.00");
        verify(customerService).deductLoyaltyPoints(customer, 100);
    }

    @Test
    @DisplayName("createSale: loyalty redemption fails when customer has insufficient points")
    void createSale_loyaltyRedemption_insufficientPoints_throwsBadRequest() {
        Customer customer = Customer.builder().id(5L).firstName("John").lastName("Doe")
                .loyaltyPoints(50).build();

        SaleRequest req = new SaleRequest(1L, 5L, PaymentMethod.CASH, null,
                List.of(new SaleRequest.SaleItemRequest(1L, 1, BigDecimal.ZERO)), null, 200);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findByEmail("cashier@test.com")).thenReturn(Optional.of(cashier));
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));
        // Validation throws before promotions are loaded — no stub needed

        assertThatThrownBy(() -> saleService.createSale(req, "cashier@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only has 50 loyalty points");
    }

    @Test
    @DisplayName("createSale: loyalty redemption without customer throws BadRequestException")
    void createSale_loyaltyRedemption_withoutCustomer_throwsBadRequest() {
        SaleRequest req = new SaleRequest(1L, null, PaymentMethod.CASH, null,
                List.of(new SaleRequest.SaleItemRequest(1L, 1, BigDecimal.ZERO)), null, 100);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findByEmail("cashier@test.com")).thenReturn(Optional.of(cashier));
        // customerId is null so customer is not loaded, then loyalty check throws — no promo stub needed

        assertThatThrownBy(() -> saleService.createSale(req, "cashier@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("customer must be selected");
    }

    @Test
    @DisplayName("createSale: unknown branch throws ResourceNotFoundException")
    void createSale_unknownBranch_throwsNotFound() {
        SaleRequest req = new SaleRequest(99L, null, PaymentMethod.CASH, null,
                List.of(new SaleRequest.SaleItemRequest(1L, 1, BigDecimal.ZERO)), null, 0);

        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.createSale(req, "cashier@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("voidSale: completed sale gets voided and inventory restored")
    void voidSale_completedSale_restoresInventory() {
        // cashier required because SaleResponse.from() calls getCashier().getFirstName()
        Sale sale = Sale.builder().id(1L).branch(branch).cashier(cashier)
                .status(SaleStatus.COMPLETED)
                .subtotal(BigDecimal.TEN).discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO).totalAmount(BigDecimal.TEN)
                .paymentMethod(PaymentMethod.CASH)
                .items(List.of(SaleItem.builder().product(product).quantity(3).build()))
                .build();

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaleResponse result = saleService.voidSale(1L);

        assertThat(result.getStatus()).isEqualTo("VOIDED");
        verify(inventoryService).addStock(product, branch, 3);
    }

    @Test
    @DisplayName("voidSale: non-completed sale throws BadRequestException")
    void voidSale_nonCompletedSale_throwsBadRequest() {
        Sale sale = Sale.builder().id(1L).branch(branch).cashier(cashier)
                .status(SaleStatus.VOIDED).items(List.of()).build();
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> saleService.voidSale(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only COMPLETED");
    }
}
