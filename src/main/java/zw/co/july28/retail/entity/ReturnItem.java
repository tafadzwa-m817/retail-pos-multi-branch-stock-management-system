package zw.co.july28.retail.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "return_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_return_id", nullable = false)
    @JsonIgnore
    private ProductReturn productReturn;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "original_sale_item_id", nullable = false)
    private SaleItem originalSaleItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitRefundAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalRefundAmount;
}
