package zw.co.july28.retail.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenShiftRequest {

    @NotNull(message = "Branch is required")
    private Long branchId;

    @NotNull(message = "Opening float is required")
    @DecimalMin(value = "0.0", message = "Opening float cannot be negative")
    private BigDecimal openingFloat;

    private String notes;
}
