package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.EncryptionService;
import com.example.bankcards.service.TransactionService;
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

@RestController
@RequestMapping("/api/transactions")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CardService cardService;

    @Autowired
    private UserService userService;

    @Autowired
    private EncryptionService encryptionService;

    // Перевод между картами по оригинальным номерам
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transferBetweenCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest transferRequest) {

        User user = userService.findByUsername(userDetails.getUsername());

        // Шифруем номера для поиска в БД
        String encryptedFromCard = encryptionService.encrypt(transferRequest.getFromCardNumber());
        String encryptedToCard = encryptionService.encrypt(transferRequest.getToCardNumber());

        // Проверяем, что обе карты принадлежат пользователю
        Card fromCard = cardService.getByCardNumberAndUser(encryptedFromCard, user);
        Card toCard = cardService.getByCardNumberAndUser(encryptedToCard, user);

        // Выполняем перевод
        Transaction transaction = transactionService.transferBetweenCards(
                fromCard, toCard, transferRequest.getAmount(), transferRequest.getDescription()
        );

        return ResponseEntity.ok(convertToTransferResponse(transaction));
    }

    // Получить историю транзакций по оригинальному номеру карты
    @GetMapping("/history")
    public ResponseEntity<Page<TransferResponse>> getCardTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String cardNumber, // Оригинальный номер
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());

        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        // Проверяем, что карта принадлежит пользователю
        cardService.getByCardNumberAndUser(encryptedCardNumber, user);

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.getCardTransactions(encryptedCardNumber, pageable);

        Page<TransferResponse> response = transactions.map(this::convertToTransferResponse);
        return ResponseEntity.ok(response);
    }

    // Получить отправленные транзакции по оригинальному номеру карты
    @GetMapping("/sent")
    public ResponseEntity<Page<TransferResponse>> getSentTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String cardNumber, // Оригинальный номер
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());

        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        cardService.getByCardNumberAndUser(encryptedCardNumber, user);

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.getSentTransactions(encryptedCardNumber, pageable);

        Page<TransferResponse> response = transactions.map(this::convertToTransferResponse);
        return ResponseEntity.ok(response);
    }

    // Получить полученные транзакции по оригинальному номеру карты
    @GetMapping("/received")
    public ResponseEntity<Page<TransferResponse>> getReceivedTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String cardNumber, // Оригинальный номер
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());

        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        cardService.getByCardNumberAndUser(encryptedCardNumber, user);

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.getReceivedTransactions(encryptedCardNumber, pageable);

        Page<TransferResponse> response = transactions.map(this::convertToTransferResponse);
        return ResponseEntity.ok(response);
    }

    // Получить транзакции за период по оригинальному номеру карты
    @GetMapping("/period")
    public ResponseEntity<Page<TransferResponse>> getTransactionsByPeriod(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String cardNumber, // Оригинальный номер
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());

        // Шифруем номер для поиска в БД
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        cardService.getByCardNumberAndUser(encryptedCardNumber, user);

        // Преобразуем строки в LocalDateTime (нужно добавить валидацию дат)
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.getTransactionsByPeriod(
                encryptedCardNumber, start, end, pageable);

        Page<TransferResponse> response = transactions.map(this::convertToTransferResponse);
        return ResponseEntity.ok(response);
    }

    // Получить транзакцию по ID
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransactionById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        // Для простоты - пользователь может видеть любую транзакцию где участвуют его карты
        // В реальном приложении нужна дополнительная проверка прав
        Transaction transaction = transactionService.findById(id);

        User user = userService.findByUsername(userDetails.getUsername());

        // Проверяем, что пользователь имеет отношение к этой транзакции
        String fromCardEncrypted = transaction.getFromCard().getCardNumber();
        String toCardEncrypted = transaction.getToCard().getCardNumber();

        // Пытаемся найти карты у пользователя
        boolean hasAccess = cardService.findByCardNumberAndUser(fromCardEncrypted, user).isPresent() ||
                cardService.findByCardNumberAndUser(toCardEncrypted, user).isPresent();

        if (!hasAccess) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(convertToTransferResponse(transaction));
    }

    // Преобразование Transaction в TransferResponse
    private TransferResponse convertToTransferResponse(Transaction transaction) {
        String fromCardMasked = cardService.getMaskedCardNumber(transaction.getFromCard());
        String toCardMasked = cardService.getMaskedCardNumber(transaction.getToCard());

        return new TransferResponse(
                transaction.getId(),
                fromCardMasked,      // ТОЛЬКО маскированные номера
                toCardMasked,
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getDescription()
        );
    }
}