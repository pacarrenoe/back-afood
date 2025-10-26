package com.app.backend.service.impl;

import com.app.backend.model.User;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.AuditService;
import com.app.backend.service.UserService;
import com.app.backend.utils.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Autowired
    private AuditService auditService;

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                logger.warn("Intento de búsqueda con username nulo o vacío");
                return Optional.empty();
            }

            Optional<User> userOpt = userRepository.findByUsername(username);
            userOpt.ifPresentOrElse(
                    u -> logger.info("Usuario encontrado: {}", username),
                    () -> logger.warn("Usuario no encontrado: {}", username)
            );
            return userOpt;

        } catch (Exception e) {
            logger.error("Error al buscar usuario por username '{}': {}", username, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> findUserResponse(String username) {
        try {
            if (username == null || username.trim().isEmpty())
                return ResponseUtil.error("El nombre de usuario es obligatorio", Map.of(), 400);

            return userRepository.findByUsername(username)
                    .map(user -> ResponseUtil.success("Usuario encontrado correctamente", Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "email", user.getEmail(),
                            "rol", user.getRol(),
                            "cargo", user.getCargo(),
                            "salario", user.getSalario(),
                            "fechaRegistro", user.getFechaRegistro()
                    )))
                    .orElse(ResponseUtil.error("Usuario no encontrado", Map.of(), 404));

        } catch (Exception e) {
            logger.error("Error al buscar usuario '{}': {}", username, e.getMessage(), e);
            return ResponseUtil.error("Error interno al buscar usuario", Map.of("detalle", e.getMessage()), 500);
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> updateUser(String username, Map<String, Object> updates) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    if (updates.containsKey("email")) user.setEmail((String) updates.get("email"));
                    if (updates.containsKey("rol")) user.setRol((String) updates.get("rol"));
                    if (updates.containsKey("cargo")) user.setCargo((String) updates.get("cargo"));
                    if (updates.containsKey("salario")) user.setSalario((Integer) updates.get("salario"));

                    userRepository.save(user);
                    auditService.logAction(username, "UPDATE_USER", "Usuario actualizado");
                    return ResponseUtil.success("Usuario actualizado correctamente", user);
                })
                .orElse(ResponseUtil.error("Usuario no encontrado", null, 404));
    }

    @Override
    public ResponseEntity<Map<String, Object>> deleteUser(String username) {
        if (!userRepository.existsByUsername(username))
            return ResponseUtil.error("Usuario no encontrado", null, 404);

        userRepository.deleteByUsername(username);
        auditService.logAction(username, "DELETE_USER", "Usuario eliminado");
        return ResponseUtil.success("Usuario eliminado correctamente", null);
    }

    @Override
    public ResponseEntity<Map<String, Object>> listUsers(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        return ResponseUtil.success("Usuarios obtenidos correctamente", Map.of(
                "content", users.getContent(),
                "totalElements", users.getTotalElements(),
                "totalPages", users.getTotalPages()
        ));
    }
}
