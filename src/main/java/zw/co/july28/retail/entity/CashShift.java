package zw.co.july28.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import zw.co.july28.retail.enums.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "opened_by_id", nullable = false)
    private AppUser openedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "closed_by_id")
    private AppUser closedBy;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal openingFloat;

    @Column(precision = 10, scale = 2)
    private BigDecimal closingCash;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalSalesAmount = BigDecimal.ZERO;

    @Builder.Default
    private int totalTransactions = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ShiftStatus status = ShiftStatus.OPEN;

    @Column(length = 500)
    private String notes;

    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        openedAt = LocalDateTime.now();
    }
}
