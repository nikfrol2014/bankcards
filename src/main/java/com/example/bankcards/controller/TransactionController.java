package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CardService cardService;

    @Autowired
    private UserService userService;

    // Перевод между своими картами
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TransferResponse> transferBetweenCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest transferRequest) {

        User user = userService.findByUsername(userDetails.getUsername());

        // Проверяем, что обе карты принадлежат пользователю
        Card fromCard = cardService.findByIdAndUser(transferRequest.getFromCardId(), user)
                .orElseThrow(() -> new RuntimeException("Source card not found or access denied"));

        Card toCard = cardService.findByIdAndUser(transferRequest.getToCardId(), user)
                .orElseThrow(() -> new RuntimeException("Destination card not found or access denied"));

        // Выполняем перевод
        Transaction transaction = transactionService.transferBetweenCards(
                fromCard, toCard, transferRequest.getAmount(), transferRequest.getDescription()
        );

        return ResponseEntity.ok(convertToTransferResponse(transaction));
    }

    // Получить историю транзакций по карте
    @GetMapping("/card/{cardId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<TransferResponse>> getCardTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.getCardTransactions(card, pageable);

        Page<TransferResponse> response = transactions.map(this::convertToTransferResponse);
        return ResponseEntity.ok(response);
    }

    // Получить отправленные транзакции
    @GetMapping("/sent/{cardId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<TransferResponse>> getSentTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.getSentTransactions(card, pageable);

        Page<TransferResponse> response = transactions.map(this::convertToTransferResponse);
        return ResponseEntity.ok(response);
    }

    // Получить полученные транзакции
    @GetMapping("/received/{cardId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<TransferResponse>> getReceivedTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.findByUsername(userDetails.getUsername());
        Card card = cardService.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.getReceivedTransactions(card, pageable);

        Page<TransferResponse> response = transactions.map(this::convertToTransferResponse);
        return ResponseEntity.ok(response);
    }

    // Преобразование Transaction в TransferResponse
    private TransferResponse convertToTransferResponse(Transaction transaction) {
        String fromCardMasked = cardService.getMaskedCardNumber(transaction.getFromCard());
        String toCardMasked = cardService.getMaskedCardNumber(transaction.getToCard());

        return new TransferResponse(
                transaction.getId(),
                transaction.getFromCard().getId(),
                transaction.getToCard().getId(),
                fromCardMasked,
                toCardMasked,
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getDescription()
        );
    }
}