package com.app.backend.controller;

import com.app.backend.service.UserService;
import com.app.backend.utils.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
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

    @GetMapping("/user/{username}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String username) {
        return userService.findUserResponse(username);
    }
}
