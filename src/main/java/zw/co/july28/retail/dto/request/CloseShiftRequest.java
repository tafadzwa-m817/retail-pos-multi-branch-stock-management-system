package zw.co.july28.retail.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CloseShiftRequest {

    @NotNull(message = "Closing cash amount is required")
    @DecimalMin(value = "0.0", message = "Closing cash cannot be negative")
    private BigDecimal closingCash;

    private String notes;
}
