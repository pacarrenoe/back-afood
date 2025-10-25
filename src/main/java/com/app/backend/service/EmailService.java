package com.app.backend.service;

public interface EmailService {
    void sendEmail(String to, String subject, String htmlContent);
}
