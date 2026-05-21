package zw.co.july28.retail.entity;

import jakarta.persistence.*;
import lombok.*;
import zw.co.july28.retail.enums.WastageReason;

import java.time.LocalDateTime;

@Entity
@Table(name = "wastage")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wastage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WastageReason reason;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private AppUser recordedBy;

    private LocalDateTime wastedAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (wastedAt == null) wastedAt = LocalDateTime.now();
    }
}
