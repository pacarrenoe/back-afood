package com.app.backend.service;

import com.app.backend.dto.LoginRequest;
import com.app.backend.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import java.util.Map;

public interface AuthService {
    ResponseEntity<Map<String, Object>> login(LoginRequest request);
    ResponseEntity<Map<String, Object>> register(RegisterRequest request);
    ResponseEntity<Map<String, Object>> recoverPassword(String email);
    ResponseEntity<Map<String, Object>> changePassword(String token, String oldPassword, String newPassword);
    ResponseEntity<Map<String, Object>> refreshToken(String refreshToken);
}
