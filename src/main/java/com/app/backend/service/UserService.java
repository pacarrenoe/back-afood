package com.app.backend.service;

import com.app.backend.model.User;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

public interface UserService {

    Optional<User> findByUsername(String username);
    ResponseEntity<Map<String, Object>> findUserResponse(String username);

}
