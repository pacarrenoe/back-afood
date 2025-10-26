package com.app.backend.service;

public interface AuditService {
    void logAction(String username, String action, String ipAddress);
}