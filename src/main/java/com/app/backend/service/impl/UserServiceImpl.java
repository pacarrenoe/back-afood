package com.app.backend.service.impl;

import com.app.backend.model.User;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.UserService;
import com.app.backend.utils.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                logger.warn("Intento de búsqueda con username nulo o vacío");
                return Optional.empty();
            }

            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                logger.warn("Usuario no encontrado para username: {}", username);
            } else {
                logger.info("Usuario encontrado: {}", username);
            }

            return userOpt;

        } catch (Exception e) {
            logger.error("Error al buscar usuario por username '{}': {}", username, e.getMessage(), e);
            return Optional.empty();
        }
    }


    public ResponseEntity<Map<String, Object>> findUserResponse(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return ResponseUtil.error("El nombre de usuario es obligatorio", Map.of(), 400);
            }

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseUtil.error("Usuario no encontrado", Map.of(), 404);
            }

            User user = userOpt.get();
            Map<String, Object> data = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "rol", user.getRol(),
                    "cargo", user.getCargo(),
                    "salario", user.getSalario(),
                    "fechaRegistro", user.getFechaRegistro()
            );

            return ResponseUtil.success("Usuario encontrado correctamente", data);

        } catch (Exception e) {
            logger.error("Error al buscar usuario '{}': {}", username, e.getMessage(), e);
            return ResponseUtil.error("Error interno al buscar usuario", Map.of("detalle", e.getMessage()), 500);
        }
    }
}
