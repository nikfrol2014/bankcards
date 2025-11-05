package com.example.bankcards.controller;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private CardService cardService;

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

        User user = userService.findUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Card> cards = cardService.getUserCards(user, pageable);
        return ResponseEntity.ok(cards);
    }

    // Блокировка карты администратором
    @PutMapping("/cards/{cardId}/block")
    public ResponseEntity<Card> blockCardAsAdmin(@PathVariable Long cardId) {
        Card card = cardService.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        // Админ может блокировать любую карту без проверки владения
        return ResponseEntity.ok(cardService.updateCardStatus(cardId, card.getStatus(), card.getUser()));
    }
}