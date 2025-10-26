package com.app.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "audit_logs")

public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String action; // LOGIN, CHANGE_PASSWORD, REGISTER, RECOVER_PASSWORD
    private LocalDateTime timestamp = LocalDateTime.now();
    private String ipAddress;

    public AuditLog() {
    }

    public AuditLog(String username, String action, String ipAddress) {
        this.username = username;
        this.action = action;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}

