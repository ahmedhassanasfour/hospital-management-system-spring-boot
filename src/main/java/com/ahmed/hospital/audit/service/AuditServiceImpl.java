package com.ahmed.hospital.audit.service;

import com.ahmed.hospital.audit.entity.AuditLog;
import com.ahmed.hospital.audit.repository.AuditLogRepository;
import com.ahmed.hospital.common.exception.ResourceNotFoundException;
import com.ahmed.hospital.user.entity.User;
import com.ahmed.hospital.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    public void log(
            Long userId,
            String action,
            String entityType,
            Long entityId,
            String description,
            String ipAddress
    ) {
        try {
            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error(
                    "Failed to record audit log: action={}, entityType={}, entityId={} — {}",
                    action,
                    entityType,
                    entityId,
                    ex.getMessage(),
                    ex
            );
        }
    }
}