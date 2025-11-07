package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CardService cardService;

    @InjectMocks private TransactionService transactionService;

    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        fromCard = new Card();
        fromCard.setCardNumber("encrypted_from");
        fromCard.setOwner("FROM USER");
        fromCard.setExpiryDate(LocalDate.now().plusYears(1));
        fromCard.setStatus(CardStatus.ACTIVE);
        fromCard.setBalance(new BigDecimal("1000.00"));

        toCard = new Card();
        toCard.setCardNumber("encrypted_to");
        toCard.setOwner("TO USER");
        toCard.setExpiryDate(LocalDate.now().plusYears(1));
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setBalance(new BigDecimal("500.00"));
    }

    @Test
    void transferBetweenCards_WithValidData_ShouldCompleteSuccessfully() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        String description = "Test transfer";

        when(cardService.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        // When
        Transaction result = transactionService.transferBetweenCards(fromCard, toCard, amount, description);

        // Then
        assertNotNull(result);
        assertEquals(fromCard, result.getFromCard());
        assertEquals(toCard, result.getToCard());
        assertEquals(amount, result.getAmount());
        assertEquals(description, result.getDescription());

        // Verify balances updated correctly
        assertEquals(new BigDecimal("900.00"), fromCard.getBalance());
        assertEquals(new BigDecimal("600.00"), toCard.getBalance());
    }

    @Test
    void transferBetweenCards_WithSameCard_ShouldThrowException() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transferBetweenCards(fromCard, fromCard, amount, "Same card")
        );
    }

    @Test
    void transferBetweenCards_WithZeroAmount_ShouldThrowException() {
        // Given
        BigDecimal zeroAmount = BigDecimal.ZERO;

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transferBetweenCards(fromCard, toCard, zeroAmount, "Zero amount")
        );
    }

    @Test
    void transferBetweenCards_WithNegativeAmount_ShouldThrowException() {
        // Given
        BigDecimal negativeAmount = new BigDecimal("-50.00");

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transferBetweenCards(fromCard, toCard, negativeAmount, "Negative amount")
        );
    }

    @Test
    void transferBetweenCards_WithBlockedCard_ShouldThrowCardBlockedException() {
        // Given
        fromCard.setStatus(CardStatus.BLOCKED);
        BigDecimal amount = new BigDecimal("100.00");

        doThrow(new CardBlockedException("Card is blocked"))
                .when(cardService).validateCardForTransaction(fromCard);

        // When & Then
        assertThrows(CardBlockedException.class, () ->
                transactionService.transferBetweenCards(fromCard, toCard, amount, "Blocked card")
        );
    }

    @Test
    void transferBetweenCards_WithInsufficientFunds_ShouldThrowInsufficientFundsException() {
        // Given
        fromCard.setBalance(new BigDecimal("50.00"));
        BigDecimal amount = new BigDecimal("100.00");

        doThrow(new InsufficientFundsException("Insufficient funds"))
                .when(cardService).validateSufficientFunds(fromCard, amount);

        // When & Then
        assertThrows(InsufficientFundsException.class, () ->
                transactionService.transferBetweenCards(fromCard, toCard, amount, "Insufficient funds")
        );
    }
}