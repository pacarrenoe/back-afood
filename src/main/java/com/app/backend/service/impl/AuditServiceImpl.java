package com.app.backend.service.impl;

import com.app.backend.model.AuditLog;
import com.app.backend.repository.AuditLogRepository;
import com.app.backend.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public void logAction(String username, String action, String ipAddress) {
        AuditLog log = new AuditLog(username, action, ipAddress);
        auditLogRepository.save(log);
    }



}