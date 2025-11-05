package com.example.bankcards.controller;

import com.example.bankcards.dto.CardRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @Autowired
    private UserService userService;

    // Получить все карты пользователя с пагинацией
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<CardResponse>> getUserCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {

        User user = userService.findByUsername(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<Card> cards = cardService.getUserCards(user, pageable);

        Page<CardResponse> response = cards.map(this::convertToCardResponse);
        return ResponseEntity.ok(response);
    }

    // Получить карту по ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponse> getCardById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));

        return ResponseEntity.ok(convertToCardResponse(card));
    }

    // Создать новую карту (только для ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody CardRequest cardRequest,
            @RequestParam Long userId) {

        User user = userService.findUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Card card = new Card();
        card.setCardNumber(cardRequest.getCardNumber());
        card.setOwner(cardRequest.getOwner());
        card.setExpiryDate(cardRequest.getExpiryDate());

        Card savedCard = cardService.createCard(card, user);
        return ResponseEntity.ok(convertToCardResponse(savedCard));
    }

    // Заблокировать карту
    @PutMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponse> blockCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.updateCardStatus(id, CardStatus.BLOCKED, user);
        return ResponseEntity.ok(convertToCardResponse(card));
    }

    // Активировать карту
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponse> activateCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.updateCardStatus(id, CardStatus.ACTIVE, user);
        return ResponseEntity.ok(convertToCardResponse(card));
    }

    // Получить баланс карты
    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BigDecimal> getCardBalance(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));

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
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCard(@PathVariable Long id) {
        // Для ADMIN можно удалять любую карту
        Card card = cardService.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        cardService.deleteCard(id, card.getUser());
        return ResponseEntity.ok().build();
    }

    // Преобразование Card в CardResponse
    private CardResponse convertToCardResponse(Card card) {
        String maskedNumber = cardService.getMaskedCardNumber(card);
        return new CardResponse(
                card.getId(),
                maskedNumber,
                card.getOwner(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance(),
                card.getUser().getId()
        );
    }
}