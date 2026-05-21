package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.AuditLog;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private String entityType;
    private Long entityId;
    private String action;
    private String performedBy;
    private String details;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction().name())
                .performedBy(log.getPerformedBy())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
