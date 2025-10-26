package com.app.backend.controller;

import com.app.backend.service.UserService;
import com.app.backend.utils.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Perfil del usuario autenticado
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> getUserProfile() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userService.findByUsername(username)
                .map(user -> ResponseUtil.success("Perfil obtenido correctamente", Map.of(
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "nombreCompleto", user.getNombreCompleto(),
                        "cargo", user.getCargo(),
                        "rol", user.getRol(),
                        "salario", user.getSalario(),
                        "fechaRegistro", user.getFechaRegistro()
                )))
                .orElseGet(() -> ResponseUtil.error("Usuario no encontrado", null, 404));
    }

    // Obtener usuario específico
    @GetMapping("/user/{username}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String username) {
        return userService.findUserResponse(username);
    }

    // Actualizar usuario
    @PutMapping("/{username}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String username,
            @RequestBody Map<String, Object> updates) {
        return userService.updateUser(username, updates);
    }

    // Eliminar usuario
    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String username) {
        return userService.deleteUser(username);
    }

    // Listado de usuarios con paginación
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return userService.listUsers(page, size);
    }
}
