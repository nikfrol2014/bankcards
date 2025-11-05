package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private EncryptionService encryptionService;

    // Создать новую карту
    public Card createCard(Card card, User user) {
        // Шифруем номер карты перед сохранением
        String encryptedCardNumber = encryptionService.encrypt(card.getCardNumber());
        card.setCardNumber(encryptedCardNumber);
        card.setUser(user);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);

        return cardRepository.save(card);
    }

    // Найти карту по ID
    public Optional<Card> findById(Long id) {
        return cardRepository.findById(id);
    }

    // Найти карту по ID и пользователю (проверка владения)
    public Optional<Card> findByIdAndUser(Long id, User user) {
        return cardRepository.findByIdAndUser(id, user);
    }

    // Получить все карты пользователя с пагинацией
    public Page<Card> getUserCards(User user, Pageable pageable) {
        return cardRepository.findByUser(user, pageable);
    }

    // Получить карты пользователя по статусу с пагинацией
    public Page<Card> getUserCardsByStatus(User user, CardStatus status, Pageable pageable) {
        return cardRepository.findByUserAndStatus(user, status, pageable);
    }

    // Поиск карт по владельцу (имя на карте)
    public Page<Card> searchUserCardsByOwner(User user, String owner, Pageable pageable) {
        return cardRepository.findByUserAndOwnerContainingIgnoreCase(user, owner, pageable);
    }

    // Обновить статус карты
    public Card updateCardStatus(Long cardId, CardStatus newStatus, User user) {
        Card card = cardRepository.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));

        card.setStatus(newStatus);
        return cardRepository.save(card);
    }

    // Обновить баланс карты
    public Card updateBalance(Long cardId, BigDecimal newBalance) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        card.setBalance(newBalance);
        return cardRepository.save(card);
    }

    // Проверить, активна ли карта
    public boolean isCardActive(Card card) {
        return card.getStatus() == CardStatus.ACTIVE &&
                !card.getExpiryDate().isBefore(LocalDate.now());
    }

    // Проверить, не истек ли срок карты
    public void checkCardExpiry(Card card) {
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
        }
    }

    // Получить замаскированный номер карты
    public String getMaskedCardNumber(Card card) {
        String decryptedNumber = encryptionService.decrypt(card.getCardNumber());
        return maskCardNumber(decryptedNumber);
    }

    // Маскирование номера карты (**** **** **** 1234)
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    // Сохранить карту
    public Card save(Card card) {
        return cardRepository.save(card);
    }

    // Удалить карту
    public void deleteCard(Long cardId, User user) {
        Card card = cardRepository.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));
        cardRepository.delete(card);
    }

    public Page<Card> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }
}
