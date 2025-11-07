package com.example.bankcards.service;

import com.example.bankcards.entity.*;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.CardExpiredException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.repository.BlockRequestRepository;
import com.example.bankcards.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private BlockRequestRepository blockRequestRepository;

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

    // Пользователь запрашивает блокировку
    public BlockRequest requestCardBlock(String cardNumber, User user, String reason) {
        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        Card card = getByCardNumberAndUser(encryptedCardNumber, user);

        // Проверяем, что карта активна
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalArgumentException("Card is not active or already blocked/pending");
        }

        // Проверяем, что нет pending запроса
        if (blockRequestRepository.existsByCardCardNumberAndStatus(encryptedCardNumber, BlockRequestStatus.PENDING)) {
            throw new IllegalArgumentException("Block request already exists for this card");
        }

        // Создаем запрос на блокировку
        BlockRequest request = new BlockRequest();
        request.setCard(card);
        request.setUser(user);
        request.setReason(reason);
        request.setStatus(BlockRequestStatus.PENDING);

        // Меняем статус карты на "ожидает блокировки"
        card.setStatus(CardStatus.PENDING_BLOCK);
        cardRepository.save(card);

        return blockRequestRepository.save(request);
    }

    // Админ одобряет блокировку
    public Card approveBlockRequest(Long requestId, User admin) {
        BlockRequest request = blockRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Block request not found with id: " + requestId));

        if (request.getStatus() != BlockRequestStatus.PENDING) {
            throw new IllegalArgumentException("Block request is not pending");
        }

        // Блокируем карту
        Card card = request.getCard();
        card.setStatus(CardStatus.BLOCKED);

        // Обновляем запрос
        request.setStatus(BlockRequestStatus.APPROVED);
        request.setProcessedDate(LocalDateTime.now());
        request.setProcessedBy(admin);

        blockRequestRepository.save(request);
        return cardRepository.save(card);
    }

    // Админ отклоняет блокировку
    public Card rejectBlockRequest(Long requestId, User admin, String rejectionReason) {
        BlockRequest request = blockRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Block request not found with id: " + requestId));

        if (request.getStatus() != BlockRequestStatus.PENDING) {
            throw new IllegalArgumentException("Block request is not pending");
        }

        // Возвращаем карту в активный статус
        Card card = request.getCard();
        card.setStatus(CardStatus.ACTIVE);

        // Обновляем запрос
        request.setStatus(BlockRequestStatus.REJECTED);
        request.setProcessedDate(LocalDateTime.now());
        request.setProcessedBy(admin);
        if (rejectionReason != null && !rejectionReason.trim().isEmpty()) {
            request.setReason(request.getReason() + " [REJECTED: " + rejectionReason + "]");
        }

        blockRequestRepository.save(request);
        return cardRepository.save(card);
    }

    // Получить pending запросы (для админа)
    public Page<BlockRequest> getPendingBlockRequests(Pageable pageable) {
        return blockRequestRepository.findByStatus(BlockRequestStatus.PENDING, pageable);
    }

    // Получить запросы пользователя
    public Page<BlockRequest> getUserBlockRequests(User user, Pageable pageable) {
        return blockRequestRepository.findByUserUsername(user.getUsername(), pageable);
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