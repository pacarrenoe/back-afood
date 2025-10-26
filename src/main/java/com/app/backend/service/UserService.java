package com.app.backend.service;

import com.app.backend.model.User;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

public interface UserService {

    Optional<User> findByUsername(String username);

    ResponseEntity<Map<String, Object>> findUserResponse(String username);

    ResponseEntity<Map<String, Object>> updateUser(String username, Map<String, Object> updates);

    ResponseEntity<Map<String, Object>> deleteUser(String username);

    ResponseEntity<Map<String, Object>> listUsers(int page, int size);
}
