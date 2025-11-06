package com.example.bankcards.controller;

import com.example.bankcards.dto.CardRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.EncryptionService;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @Autowired
    private UserService userService;

    @Autowired
    private EncryptionService encryptionService;

    // Получить все карты пользователя с пагинацией
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<CardResponse>> getUserCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "owner") String sort) {

        User user = userService.findByUsername(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<Card> cards = cardService.getUserCards(user, pageable);

        Page<CardResponse> response = cards.map(this::convertToCardResponse);
        return ResponseEntity.ok(response);
    }

    // Получить карту по номеру
    @GetMapping("/{cardNumber}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponse> getCardByNumber(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardNumber) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.getByCardNumberAndUser(cardNumber, user);
        return ResponseEntity.ok(convertToCardResponse(card));
    }

    // Обновить баланс карты по оригинальному номеру
    @PutMapping("/balance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponse> updateCardBalance(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String cardNumber, // Оригинальный номер (4111111111111111)
            @RequestParam @DecimalMin(value = "0.00", message = "Balance must be positive") BigDecimal newBalance) {

        User user = userService.findByUsername(userDetails.getUsername());

        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        Card card = cardService.updateCardBalance(encryptedCardNumber, newBalance, user);
        return ResponseEntity.ok(convertToCardResponse(card));
    }

    // Получить карту по оригинальному номеру
    @GetMapping("/by-number")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponse> getCardByOriginalNumber(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String cardNumber) {

        User user = userService.findByUsername(userDetails.getUsername());

        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        Card card = cardService.getByCardNumberAndUser(encryptedCardNumber, user);
        return ResponseEntity.ok(convertToCardResponse(card));
    }

    // Создать новую карту (только для ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody CardRequest cardRequest,
            @RequestParam Long userId) {

        User user = userService.getUserById(userId);

        Card card = new Card();
        card.setCardNumber(cardRequest.getCardNumber()); // Оригинальный номер
        card.setOwner(cardRequest.getOwner());
        card.setExpiryDate(cardRequest.getExpiryDate());

        Card savedCard = cardService.createCard(card, user);
        return ResponseEntity.ok(convertToCardResponse(savedCard));
    }

    // Заблокировать карту
    @PutMapping("/{cardNumber}/block")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponse> blockCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardNumber) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.updateCardStatus(cardNumber, CardStatus.BLOCKED, user);
        return ResponseEntity.ok(convertToCardResponse(card));
    }

    // Активировать карту
    @PutMapping("/{cardNumber}/activate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponse> activateCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardNumber) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.updateCardStatus(cardNumber, CardStatus.ACTIVE, user);
        return ResponseEntity.ok(convertToCardResponse(card));
    }

    // Получить баланс карты
    @GetMapping("/{cardNumber}/balance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BigDecimal> getCardBalance(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardNumber) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.getByCardNumberAndUser(cardNumber, user);
        return ResponseEntity.ok(card.getBalance());
    }

    // Поиск карт по владельцу (имя на карте)
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<CardResponse>> searchCardsByOwner(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String owner,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size);
        Page<Card> cards = cardService.searchUserCardsByOwner(user, owner, pageable);

        Page<CardResponse> response = cards.map(this::convertToCardResponse);
        return ResponseEntity.ok(response);
    }

    // Удалить карту (только для ADMIN)
    @DeleteMapping("/{cardNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCard(@PathVariable String cardNumber) {
        // Админ может удалить любую карту без проверки владения
        Card card = cardService.getByCardNumber(cardNumber);
        cardService.deleteCard(cardNumber, card.getUser());
        return ResponseEntity.ok().build();
    }

    // Преобразование Card в CardResponse
    private CardResponse convertToCardResponse(Card card) {
        String maskedNumber = cardService.getMaskedCardNumber(card);
        return new CardResponse(
                card.getCardNumber(), // Теперь это зашифрованный номер карты
                maskedNumber,
                card.getOwner(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance(),
                card.getUser().getId()
        );
    }
}