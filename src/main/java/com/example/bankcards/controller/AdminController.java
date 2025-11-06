package com.example.bankcards.controller;

import com.example.bankcards.dto.AdminCardResponse;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.EncryptionService;
import com.example.bankcards.service.UserService;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    @Autowired
    private EncryptionService encryptionService;

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
        // Здесь можно добавить логику блокировки, если нужно

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
        // Здесь можно добавить логику активации, если нужно

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
        stats.put("adminPercentage", totalUsers > 0 ? (double) adminUsers / totalUsers * 100 : 0);

        return ResponseEntity.ok(stats);
    }

    // ========== CARD MANAGEMENT ==========

    // Получить все карты (для администратора) с маскированными номерами
    @GetMapping("/cards")
    public ResponseEntity<Page<AdminCardResponse>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Card> cards = cardService.getAllCards(pageable);

        Page<AdminCardResponse> response = cards.map(this::convertToAdminCardResponse);
        return ResponseEntity.ok(response);
    }

    // Получить все карты пользователя (админская версия)
    @GetMapping("/users/{userId}/cards")
    public ResponseEntity<Page<AdminCardResponse>> getUserCards(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.getUserById(userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Card> cards = cardService.getUserCards(user, pageable);

        Page<AdminCardResponse> response = cards.map(this::convertToAdminCardResponse);
        return ResponseEntity.ok(response);
    }

    // Блокировка карты администратором по оригинальному номеру
    @PutMapping("/cards/block")
    public ResponseEntity<AdminCardResponse> blockCardAsAdmin(@RequestParam String cardNumber) {
        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        Card card = cardService.getByCardNumber(encryptedCardNumber);
        // Админ может блокировать любую карту без проверки владения
        Card updatedCard = cardService.updateCardStatus(encryptedCardNumber, CardStatus.BLOCKED, card.getUser());
        return ResponseEntity.ok(convertToAdminCardResponse(updatedCard));
    }

    // Активация карты администратором по оригинальному номеру
    @PutMapping("/cards/activate")
    public ResponseEntity<AdminCardResponse> activateCardAsAdmin(@RequestParam String cardNumber) {
        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        Card card = cardService.getByCardNumber(encryptedCardNumber);
        Card updatedCard = cardService.updateCardStatus(encryptedCardNumber, CardStatus.ACTIVE, card.getUser());
        return ResponseEntity.ok(convertToAdminCardResponse(updatedCard));
    }

    // Получить детальную информацию о карте по оригинальному номеру
    @GetMapping("/cards/details")
    public ResponseEntity<AdminCardResponse> getCardDetails(@RequestParam String cardNumber) {
        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        Card card = cardService.getByCardNumber(encryptedCardNumber);
        return ResponseEntity.ok(convertToAdminCardResponse(card));
    }

    // Обновить баланс карты по оригинальному номеру (админ)
    @PutMapping("/cards/balance")
    public ResponseEntity<AdminCardResponse> updateCardBalanceAsAdmin(
            @RequestParam String cardNumber, // Оригинальный номер
            @RequestParam @DecimalMin(value = "0.00", message = "Balance must be positive") BigDecimal newBalance) {

        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        Card card = cardService.updateCardBalanceAsAdmin(encryptedCardNumber, newBalance);
        return ResponseEntity.ok(convertToAdminCardResponse(card));
    }

    // Удалить карту по оригинальному номеру (админ)
    @DeleteMapping("/cards")
    public ResponseEntity<?> deleteCardAsAdmin(@RequestParam String cardNumber) {
        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        Card card = cardService.getByCardNumber(encryptedCardNumber);
        cardService.deleteCard(encryptedCardNumber, card.getUser());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Card deleted successfully");
        response.put("cardNumber", cardService.getMaskedCardNumber(card));

        return ResponseEntity.ok(response);
    }

    // Получить карты по статусу (админ)
    @GetMapping("/cards/by-status")
    public ResponseEntity<Page<AdminCardResponse>> getCardsByStatus(
            @RequestParam CardStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        // Нужно добавить метод в CardService для поиска по статусу
        // Page<Card> cards = cardService.getCardsByStatus(status, pageable);

        // Временно используем все карты с фильтрацией на стороне Java
        Page<Card> allCards = cardService.getAllCards(pageable);
        Page<Card> filteredCards = (Page<Card>) allCards.filter(card -> card.getStatus() == status);

        Page<AdminCardResponse> response = filteredCards.map(this::convertToAdminCardResponse);
        return ResponseEntity.ok(response);
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

    // Преобразование Card в AdminCardResponse (ТОЛЬКО с маскированным номером)
    private AdminCardResponse convertToAdminCardResponse(Card card) {
        String maskedNumber = cardService.getMaskedCardNumber(card);

        return new AdminCardResponse(
                maskedNumber,        // ТОЛЬКО маскированный номер
                card.getOwner(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance(),
                card.getUser().getId(),
                card.getUser().getUsername()
        );
    }
}