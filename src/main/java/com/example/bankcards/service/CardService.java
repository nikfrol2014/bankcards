package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.CardExpiredException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private EncryptionService encryptionService;

    // Создать новую карту с шифрованием номера
    public Card createCard(Card card, User user) {
        String encryptedCardNumber = encryptionService.encrypt(card.getCardNumber());
        card.setCardNumber(encryptedCardNumber);
        card.setUser(user);
        card.setStatus(CardStatus.ACTIVE);

        // Используем переданный баланс или устанавливаем 1000.00 по умолчанию
        if (card.getBalance() == null) {
            card.setBalance(new BigDecimal("1000.00"));
        }

        return cardRepository.save(card);
    }

    // Найти карту по номеру (зашифрованному)
    public Optional<Card> findByCardNumber(String cardNumber) {
        return cardRepository.findById(cardNumber);
    }

    // Найти карту по номеру и пользователю (проверка владения)
    public Optional<Card> findByCardNumberAndUser(String cardNumber, User user) {
        return cardRepository.findByCardNumberAndUser(cardNumber, user);
    }

    // Получить карту по номеру или выбросить исключение
    public Card getByCardNumber(String cardNumber) {
        return cardRepository.findById(cardNumber)
                .orElseThrow(() -> new CardNotFoundException("Card not found with number: " + cardNumber));
    }

    // Получить карту по номеру и пользователю или выбросить исключение
    public Card getByCardNumberAndUser(String cardNumber, User user) {
        return cardRepository.findByCardNumberAndUser(cardNumber, user)
                .orElseThrow(() -> {
                    String masked = getMaskedFallback(cardNumber);
                    return new CardNotFoundException("Card not found: " + masked + " - access denied");
                });
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
    public Card updateCardStatus(String cardNumber, CardStatus newStatus, User user) {
        Card card = getByCardNumberAndUser(cardNumber, user);
        card.setStatus(newStatus);
        return cardRepository.save(card);
    }

    // Обновить баланс карты
    public Card updateBalance(String cardNumber, BigDecimal newBalance) {
        Card card = getByCardNumber(cardNumber);
        card.setBalance(newBalance);
        return cardRepository.save(card);
    }

    // Проверить, активна ли карта (без исключений)
    public boolean isCardActive(Card card) {
        return card.getStatus() == CardStatus.ACTIVE &&
                !card.getExpiryDate().isBefore(LocalDate.now());
    }

    // Валидация карты для транзакции (с исключениями)
    public void validateCardForTransaction(Card card) {
        String maskedNumber = getMaskedCardNumber(card);

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new CardBlockedException("Card is blocked: " + maskedNumber);
        }
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardExpiredException("Card is expired: " + maskedNumber);
        }
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new CardBlockedException("Card is not active: " + maskedNumber);
        }
    }

    // Fallback маскирование для случаев, когда карта не найдена
    private String getMaskedFallback(String cardNumber) {
        try {
            // Пытаемся расшифровать и замаскировать
            String decrypted = encryptionService.decrypt(cardNumber);
            return maskCardNumber(decrypted);
        } catch (Exception e) {
            // Если не получается, используем общую маску
            return "**** **** **** ****";
        }
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
        try {
            // Пытаемся расшифровать номер карты
            String decryptedNumber = encryptionService.decrypt(card.getCardNumber());
            return maskCardNumber(decryptedNumber);
        } catch (Exception e) {
            // Если не получается расшифровать, используем fallback
            return maskCardNumber("0000" + card.getCardNumber().hashCode() % 10000);
        }
    }

    // Получить оригинальный номер карты (для админа)
    public String getDecryptedCardNumber(Card card) {
        try {
            return encryptionService.decrypt(card.getCardNumber());
        } catch (Exception e) {
            return "DECRYPTION_ERROR";
        }
    }

    // Обновить баланс карты (для админа или владельца)
    public Card updateCardBalance(String cardNumber, BigDecimal newBalance, User user) {
        Card card = getByCardNumberAndUser(cardNumber, user);
        card.setBalance(newBalance);
        return cardRepository.save(card);
    }

    // Обновить баланс карты (только для админа - без проверки владения)
    public Card updateCardBalanceAsAdmin(String cardNumber, BigDecimal newBalance) {
        Card card = getByCardNumber(cardNumber);
        card.setBalance(newBalance);
        return cardRepository.save(card);
    }

    // Маскирование номера карты (**** **** **** 1234)
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "**** **** **** ****";
        }

        // Оставляем только цифры
        String digitsOnly = cardNumber.replaceAll("[^0-9]", "");

        if (digitsOnly.length() < 4) {
            return "**** **** **** ****";
        }

        String lastFour = digitsOnly.substring(digitsOnly.length() - 4);
        return "**** **** **** " + lastFour;
    }

    // Проверка достаточности средств
    public void validateSufficientFunds(Card card, BigDecimal amount) {
        if (card.getBalance().compareTo(amount) < 0) {
            String maskedNumber = getMaskedCardNumber(card);
            throw new InsufficientFundsException(
                    String.format("Insufficient funds on card %s: balance=%.2f, required=%.2f",
                            maskedNumber, card.getBalance().doubleValue(), amount.doubleValue())
            );
        }
    }

    // Сохранить карту
    public Card save(Card card) {
        return cardRepository.save(card);
    }

    // Удалить карту
    public void deleteCard(String cardNumber, User user) {
        Card card = getByCardNumberAndUser(cardNumber, user);
        cardRepository.delete(card);
    }

    // Получить все карты (для админа)
    public Page<Card> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    // Проверить существование карты по номеру
    public boolean existsByCardNumber(String cardNumber) {
        return cardRepository.existsByCardNumber(cardNumber);
    }
}