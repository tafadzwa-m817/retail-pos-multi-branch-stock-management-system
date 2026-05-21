package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.dto.response.AuditLogResponse;
import zw.co.july28.retail.dto.response.PageResponse;
import zw.co.july28.retail.entity.AuditLog;
import zw.co.july28.retail.enums.AuditAction;
import zw.co.july28.retail.repository.AuditLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(String entityType, Long entityId, AuditAction action, String details) {
        String performedBy = resolveCurrentUser();
        AuditLog entry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }

    private String resolveCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "system";
    }

    public PageResponse<AuditLogResponse> getAll(Pageable pageable) {
        return PageResponse.from(auditLogRepository.findAll(pageable).map(AuditLogResponse::from));
    }

    public PageResponse<AuditLogResponse> getByEntityType(String entityType, Pageable pageable) {
        return PageResponse.from(auditLogRepository.findByEntityType(entityType, pageable).map(AuditLogResponse::from));
    }

    public PageResponse<AuditLogResponse> getByUser(String email, Pageable pageable) {
        return PageResponse.from(auditLogRepository.findByPerformedBy(email, pageable).map(AuditLogResponse::from));
    }

    public List<AuditLogResponse> getByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(AuditLogResponse::from)
                .collect(Collectors.toList());
    }

    public PageResponse<AuditLogResponse> getByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return PageResponse.from(auditLogRepository.findByCreatedAtBetween(start, end, pageable).map(AuditLogResponse::from));
    }
}
