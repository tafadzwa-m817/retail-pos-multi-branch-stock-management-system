package zw.co.july28.retail.dto.request;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StoreSettingsRequest {
    private String storeName;
    private String storePhone;
    private String storeAddress;
    private String logoUrl;
    private String receiptFooterText;
    private String currency;
}
