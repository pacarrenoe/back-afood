package com.app.backend.service.impl;

import com.app.backend.dto.LoginRequest;
import com.app.backend.dto.RegisterRequest;
import com.app.backend.model.User;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.JwtUtil;
import com.app.backend.service.AuthService;
import com.app.backend.service.EmailService;
import com.app.backend.utils.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public ResponseEntity<Map<String, Object>> login(LoginRequest request) {
        try {
            // Buscar por username o por email
            Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail());
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByEmail(request.getUsernameOrEmail());
            }

            if (userOpt.isEmpty()) {
                return ResponseUtil.error("Usuario o correo no encontrado", Map.of(), 404);
            }

            User user = userOpt.get();

            // Validar contraseña
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                Map<String, Object> errorData = Map.of("error", "Usuario o contraseña incorrectos");
                return ResponseUtil.error("Credenciales inválidas", errorData, 401);
            }

            // Generar token JWT
            String token = jwtUtil.generateToken(user.getUsername());

            // Si el usuario tiene una contraseña temporal → forzar cambio
            if (user.isMustChangePassword()) {
                Map<String, Object> data = Map.of(
                        "token", token,
                        "username", user.getUsername(),
                        "mustChangePassword", true,
                        "message", "Debes cambiar tu contraseña temporal antes de continuar"
                );
                return ResponseUtil.success("Login exitoso (requiere cambio de contraseña)", data);
            }

            // Login normal
            Map<String, Object> data = Map.of(
                    "token", token,
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "nombreCompleto", user.getNombreCompleto(),
                    "cargo", user.getCargo(),
                    "rol", user.getRol(),
                    "salario", user.getSalario(),
                    "fechaRegistro", user.getFechaRegistro(),
                    "mustChangePassword", false
            );

            return ResponseUtil.success("Login exitoso", data);

        } catch (Exception e) {
            return ResponseUtil.error("Error en el proceso de login",
                    Map.of("detalle", e.getMessage()), 500);
        }
    }




    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<Map<String, Object>> register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseUtil.error("Usuario ya existe", Map.of(), 400);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNombreCompleto(request.getNombreCompleto());
        user.setCargo(request.getCargo());
        user.setRol(request.getCargo().equalsIgnoreCase("gerente") ? "admin" : "user");
        user.setSalario(request.getSalario());
        user.setFechaRegistro(LocalDateTime.now());

        userRepository.save(user);

        return ResponseUtil.success("Usuario registrado exitosamente", Map.of(
                "username", user.getUsername(),
                "rol", user.getRol(),
                "cargo", user.getCargo()
        ));
    }

    @Autowired
    private EmailService emailService;

    @Override
    public ResponseEntity<Map<String, Object>> recoverPassword(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseUtil.error("El correo electrónico es obligatorio", Map.of(), 400);
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseUtil.error("Correo no encontrado", Map.of(), 404);
            }

            User user = userOpt.get();
            String tempPassword = UUID.randomUUID().toString().substring(0, 8);
            user.setPassword(passwordEncoder.encode(tempPassword));
            user.setMustChangePassword(true);
            userRepository.save(user);

            // Cargar plantilla y reemplazar variables
            String htmlTemplate = loadEmailTemplate("email-recover.html")
                    .replace("${nombreCompleto}", user.getNombreCompleto())
                    .replace("${tempPassword}", tempPassword);

            // Enviar correo HTML
            emailService.sendEmail(
                    user.getEmail(),
                    "🔐 Restablecimiento de contraseña - AppFood",
                    htmlTemplate
            );

            return ResponseUtil.success("Contraseña restablecida y enviada por correo.",
                    Map.of("email", user.getEmail()));

        } catch (Exception e) {
            return ResponseUtil.error("Error al restablecer contraseña",
                    Map.of("detalle", e.getMessage()), 500);
        }
    }

    private String loadEmailTemplate(String filename) {
        try (Scanner scanner = new Scanner(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("templates/" + filename)),
                "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo cargar la plantilla de correo: " + filename, e);
        }
    }


    @Override
    public ResponseEntity<Map<String, Object>> changePassword(String token, String oldPassword, String newPassword) {
        try {
            String username = jwtUtil.extractUsername(token);
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseUtil.error("Usuario no encontrado", Map.of(), 404);
            }

            User user = userOpt.get();

            // Si el usuario está obligado a cambiar contraseña, no pedimos la anterior
            if (user.isMustChangePassword()) {
                if (passwordEncoder.matches(newPassword, user.getPassword())) {
                    return ResponseUtil.error("La nueva contraseña no puede ser igual a la temporal", Map.of(), 400);
                }

                user.setPassword(passwordEncoder.encode(newPassword));
                user.setMustChangePassword(false);
                userRepository.save(user);

                return ResponseUtil.success("Contraseña actualizada correctamente", Map.of(
                        "username", user.getUsername(),
                        "mustChangePassword", false
                ));
            }

            // Si no está en modo obligatorio, requiere la antigua
            if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ResponseUtil.error("Contraseña anterior incorrecta", Map.of(), 400);
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return ResponseUtil.success("Contraseña cambiada correctamente", Map.of(
                    "username", user.getUsername(),
                    "mustChangePassword", false
            ));

        } catch (Exception e) {
            return ResponseUtil.error("Error al cambiar la contraseña",
                    Map.of("detalle", e.getMessage()), 500);
        }
    }






}
