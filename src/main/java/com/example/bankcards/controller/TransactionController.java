package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
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

    // Получить историю транзакций по карте
    @GetMapping("/card/{cardNumber}")
    public ResponseEntity<Page<TransferResponse>> getCardTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());
        // Проверяем, что карта принадлежит пользователю
        cardService.getByCardNumberAndUser(cardNumber, user);

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.getCardTransactions(cardNumber, pageable);

        Page<TransferResponse> response = transactions.map(this::convertToTransferResponse);
        return ResponseEntity.ok(response);
    }

    // Получить отправленные транзакции
    @GetMapping("/sent/{cardNumber}")
    public ResponseEntity<Page<TransferResponse>> getSentTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());
        cardService.getByCardNumberAndUser(cardNumber, user);

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.getSentTransactions(cardNumber, pageable);

        Page<TransferResponse> response = transactions.map(this::convertToTransferResponse);
        return ResponseEntity.ok(response);
    }

    // Получить полученные транзакции
    @GetMapping("/received/{cardNumber}")
    public ResponseEntity<Page<TransferResponse>> getReceivedTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());
        cardService.getByCardNumberAndUser(cardNumber, user);

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.getReceivedTransactions(cardNumber, pageable);

        Page<TransferResponse> response = transactions.map(this::convertToTransferResponse);
        return ResponseEntity.ok(response);
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