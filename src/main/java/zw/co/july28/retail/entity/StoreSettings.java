package zw.co.july28.retail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "store_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSettings {

    @Id
    private Long id = 1L;

    @Builder.Default
    private String storeName = "Retail POS";

    @Builder.Default
    private String storePhone = "+263 242 000000";

    @Builder.Default
    private String storeAddress = "Harare, Zimbabwe";

    private String logoUrl;

    @Builder.Default
    private String receiptFooterText = "Thank you for shopping with us!";

    @Builder.Default
    private String currency = "USD";
}
