package com.example.bankcards.controller;

import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private CardService cardService;

    // ========== USER MANAGEMENT ==========

    // Получить всех пользователей с пагинацией
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<User> users = userService.getAllUsers(pageable);

        Page<UserResponse> response = users.map(this::convertToUserResponse);
        return ResponseEntity.ok(response);
    }

    // Получить пользователя по ID
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(convertToUserResponse(user));
    }

    // Поиск пользователей по username
    @GetMapping("/users/search")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userService.searchUsersByUsername(username, pageable);

        Page<UserResponse> response = users.map(this::convertToUserResponse);
        return ResponseEntity.ok(response);
    }

    // Обновить роль пользователя
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long userId,
                                            @RequestParam Role newRole) {
        User user = userService.getUserById(userId);
        user.setRole(newRole);
        User updatedUser = userService.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User role updated successfully");
        response.put("userId", userId.toString());
        response.put("newRole", newRole.name());

        return ResponseEntity.ok(response);
    }

    // Заблокировать пользователя
    @PutMapping("/users/{userId}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        // Здесь можно добавить логику блокировки
        // Например, установить поле isActive = false

        Map<String, String> response = new HashMap<>();
        response.put("message", "User blocked successfully");
        response.put("userId", userId.toString());
        response.put("username", user.getUsername());

        return ResponseEntity.ok(response);
    }

    // Активировать пользователя
    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        // Активация пользователя

        Map<String, String> response = new HashMap<>();
        response.put("message", "User activated successfully");
        response.put("userId", userId.toString());
        response.put("username", user.getUsername());

        return ResponseEntity.ok(response);
    }

    // Получить статистику по пользователям
    @GetMapping("/users/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        long totalUsers = userService.getTotalUsersCount();
        long adminUsers = userService.getUsersCountByRole(Role.ROLE_ADMIN);
        long regularUsers = userService.getUsersCountByRole(Role.ROLE_USER);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("adminUsers", adminUsers);
        stats.put("regularUsers", regularUsers);
        stats.put("adminPercentage", (double) adminUsers / totalUsers * 100);

        return ResponseEntity.ok(stats);
    }

    // ========== CARD MANAGEMENT ==========

    // Получить все карты (для администратора)
    @GetMapping("/cards")
    public ResponseEntity<Page<Card>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Card> cards = cardService.getAllCards(pageable);
        return ResponseEntity.ok(cards);
    }

    // Получить все карты пользователя (админская версия)
    @GetMapping("/users/{userId}/cards")
    public ResponseEntity<Page<Card>> getUserCards(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.getUserById(userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Card> cards = cardService.getUserCards(user, pageable);
        return ResponseEntity.ok(cards);
    }

    // Блокировка карты администратором
    @PutMapping("/cards/{cardId}/block")
    public ResponseEntity<Card> blockCardAsAdmin(@PathVariable Long cardId) {
        Card card = cardService.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        // Админ может блокировать любую карту без проверки владения
        return ResponseEntity.ok(cardService.updateCardStatus(cardId, CardStatus.BLOCKED, card.getUser()));
    }

    // Активация карты администратором
    @PutMapping("/cards/{cardId}/activate")
    public ResponseEntity<Card> activateCardAsAdmin(@PathVariable Long cardId) {
        Card card = cardService.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        return ResponseEntity.ok(cardService.updateCardStatus(cardId, CardStatus.ACTIVE, card.getUser()));
    }

    // Преобразование User в UserResponse
    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}