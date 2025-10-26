package com.app.backend.service.impl;

import com.app.backend.dto.LoginRequest;
import com.app.backend.dto.RegisterRequest;
import com.app.backend.model.User;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.JwtUtil;
import com.app.backend.service.AuditService;
import com.app.backend.service.AuthService;
import com.app.backend.service.EmailService;
import com.app.backend.utils.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuditService auditService;

    @Value("${app.security.max-failed-attempts}") private int MAX_FAILED_ATTEMPTS;
    @Value("${app.security.lock-time-minutes}") private int LOCK_TIME_MINUTES;

    private final Map<String, LocalDateTime> recoverAttempts = new ConcurrentHashMap<>();

    @Override
    public ResponseEntity<Map<String, Object>> login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) userOpt = userRepository.findByEmail(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) return ResponseUtil.error("Usuario o correo no encontrado", Map.of(), 404);

        User user = userOpt.get();

        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            long minutosRestantes = ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getAccountLockedUntil());
            return ResponseUtil.error("Cuenta bloqueada temporalmente. Intenta en " + minutosRestantes + " minutos.", Map.of(), 423);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);

            if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
                user.setFailedAttempts(0);
                userRepository.save(user);
                return ResponseUtil.error("Cuenta bloqueada por exceso de intentos. Espera " + LOCK_TIME_MINUTES + " minutos.", Map.of(), 423);
            }

            userRepository.save(user);
            return ResponseUtil.error("Contraseña incorrecta. Intento " + user.getFailedAttempts() + " de " + MAX_FAILED_ATTEMPTS, Map.of(), 401);
        }

        user.setFailedAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        auditService.logAction(user.getUsername(), "LOGIN", "Acceso exitoso");

        String token = jwtUtil.generateToken(user.getUsername());
        String refresh = jwtUtil.generateRefreshToken(user.getUsername());
        return ResponseUtil.success("Login exitoso", Map.of(
                "token", token,
                "refreshToken", refresh,
                "username", user.getUsername(),
                "rol", user.getRol()
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent())
            return ResponseUtil.error("Usuario ya existe", Map.of(), 400);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNombreCompleto(request.getNombreCompleto());
        user.setCargo(request.getCargo());
        user.setRol(request.getCargo().equalsIgnoreCase("gerente") ? "ADMIN" : "USER");
        user.setSalario(request.getSalario());
        user.setFechaRegistro(LocalDateTime.now());

        userRepository.save(user);
        auditService.logAction(user.getUsername(), "REGISTER", null);

        return ResponseUtil.success("Usuario registrado exitosamente", Map.of(
                "username", user.getUsername(),
                "rol", user.getRol()
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> recoverPassword(String email) {
        LocalDateTime lastAttempt = recoverAttempts.get(email);
        if (lastAttempt != null && ChronoUnit.MINUTES.between(lastAttempt, LocalDateTime.now()) < 5)
            return ResponseUtil.error("Demasiadas solicitudes, intenta en 5 minutos", Map.of(), 429);

        recoverAttempts.put(email, LocalDateTime.now());

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseUtil.error("Correo no encontrado", Map.of(), 404);

        User user = userOpt.get();
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);

        String html = loadEmailTemplate("email-recover.html")
                .replace("${nombreCompleto}", user.getNombreCompleto())
                .replace("${tempPassword}", tempPassword);

        emailService.sendEmail(user.getEmail(), "Restablecimiento de contraseña - AppFood", html);
        auditService.logAction(user.getUsername(), "RECOVER_PASSWORD", null);

        return ResponseUtil.success("Contraseña temporal enviada por correo.", Map.of("email", user.getEmail()));
    }

    private String loadEmailTemplate(String filename) {
        try (Scanner scanner = new Scanner(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("templates/" + filename)),
                "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo cargar plantilla: " + filename, e);
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> changePassword(String token, String oldPassword, String newPassword) {
        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword()))
            return ResponseUtil.error("Contraseña anterior incorrecta", Map.of(), 400);

        if (passwordEncoder.matches(newPassword, user.getPassword()))
            return ResponseUtil.error("La nueva contraseña no puede ser igual a la anterior", Map.of(), 400);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        auditService.logAction(username, "CHANGE_PASSWORD", null);

        return ResponseUtil.success("Contraseña cambiada correctamente", Map.of("username", username));
    }

    @Override
    public ResponseEntity<Map<String, Object>> refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken))
            return ResponseUtil.error("Refresh token inválido o expirado", Map.of(), 401);

        String username = jwtUtil.extractUsername(refreshToken);
        String newAccessToken = jwtUtil.generateToken(username);
        return ResponseUtil.success("Token renovado exitosamente", Map.of("accessToken", newAccessToken));
    }
}
