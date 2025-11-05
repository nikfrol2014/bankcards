package com.example.bankcards.controller;

import com.example.bankcards.dto.AuthRequest;
import com.example.bankcards.dto.AuthResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.PasswordEncoderUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoderUtil passwordEncoderUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String jwt = jwtUtil.generateToken(userDetails);

            User user = userService.findByUsername(userDetails.getUsername());

            AuthResponse response = new AuthResponse(jwt, user.getUsername(), user.getRole().name());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed");
            error.put("message", "Invalid username or password");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody AuthRequest authRequest) {
        try {
            // Проверяем существование пользователя
            if (userService.userExists(authRequest.getUsername())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Registration failed");
                error.put("message", "Username already exists");
                return ResponseEntity.badRequest().body(error);
            }

            String email = authRequest.getUsername() + "@bank.com";
            if (userService.emailExists(email)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Registration failed");
                error.put("message", "Email already exists");
                return ResponseEntity.badRequest().body(error);
            }

            // Создаем нового пользователя
            User newUser = new User();
            newUser.setUsername(authRequest.getUsername());
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoderUtil.encode(authRequest.getPassword()));
            newUser.setRole(Role.ROLE_USER); // Все новые пользователи - обычные USER

            User savedUser = userService.save(newUser);

            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", savedUser.getUsername());
            response.put("role", savedUser.getRole().name());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed");
            error.put("message", "Internal server error");
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Эндпоинт для проверки токена (опционально)
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.substring(7); // Убираем "Bearer "
            String username = jwtUtil.extractUsername(jwt);
            UserDetails userDetails = userService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Token is valid");
                response.put("username", username);
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Token validation failed");
                error.put("message", "Token is invalid or expired");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token validation failed");
            error.put("message", "Invalid token format");
            return ResponseEntity.badRequest().body(error);
        }
    }
}
