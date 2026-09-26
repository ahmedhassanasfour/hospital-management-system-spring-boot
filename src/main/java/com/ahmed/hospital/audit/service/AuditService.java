package com.ahmed.hospital.audit.service;

public interface AuditService {

    void log(
            Long userId,
            String action,
            String entityType,
            Long entityId,
            String description,
            String ipAddress
    );
}