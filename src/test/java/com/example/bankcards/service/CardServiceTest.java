package com.example.bankcards.service;

import com.example.bankcards.entity.*;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.BlockRequestRepository;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private BlockRequestRepository blockRequestRepository;

    @InjectMocks private CardService cardService;

    private User testUser;
    private Card testCard;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@bank.com", "password", Role.ROLE_USER);
        testUser.setId(1L);

        testCard = new Card();
        testCard.setCardNumber("encrypted123");
        testCard.setOwner("TEST USER");
        testCard.setExpiryDate(LocalDate.now().plusYears(2));
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setBalance(new BigDecimal("1000.00"));
        testCard.setUser(testUser);
    }

    @Test
    void createCard_ShouldEncryptCardNumberAndSetDefaultBalance() {
        // Given
        String originalNumber = "4111111111111111";
        String encryptedNumber = "encrypted123";

        Card inputCard = new Card();
        inputCard.setCardNumber(originalNumber);
        inputCard.setOwner("TEST USER");
        inputCard.setExpiryDate(LocalDate.now().plusYears(2));

        when(encryptionService.encrypt(originalNumber)).thenReturn(encryptedNumber);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When
        Card result = cardService.createCard(inputCard, testUser);

        // Then
        assertNotNull(result);
        assertEquals(encryptedNumber, result.getCardNumber());
        assertEquals(CardStatus.ACTIVE, result.getStatus());
        assertEquals(testUser, result.getUser());
        verify(encryptionService).encrypt(originalNumber);
    }

    @Test
    void getByCardNumber_WhenCardNotFound_ShouldThrowException() {
        // Given
        String encryptedNumber = "nonexistent";
        when(cardRepository.findById(encryptedNumber)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CardNotFoundException.class, () ->
                cardService.getByCardNumber(encryptedNumber)
        );
    }

    @Test
    void validateCardForTransaction_WithActiveCard_ShouldNotThrow() {
        // Given
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setExpiryDate(LocalDate.now().plusYears(1));

        // When & Then
        assertDoesNotThrow(() -> cardService.validateCardForTransaction(testCard));
    }

    @Test
    void validateCardForTransaction_WithBlockedCard_ShouldThrowCardBlockedException() {
        // Given
        testCard.setStatus(CardStatus.BLOCKED);

        // When & Then
        assertThrows(CardBlockedException.class, () ->
                cardService.validateCardForTransaction(testCard)
        );
    }

    @Test
    void validateCardForTransaction_WithExpiredCard_ShouldThrowCardExpiredException() {
        // Given
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setExpiryDate(LocalDate.now().minusDays(1));

        // When & Then
        assertThrows(CardExpiredException.class, () ->
                cardService.validateCardForTransaction(testCard)
        );
    }

    @Test
    void getMaskedCardNumber_ShouldReturnCorrectMaskFormat() {
        // Given
        String encryptedNumber = "encrypted123";
        String decryptedNumber = "4111111111111111";
        testCard.setCardNumber(encryptedNumber);

        when(encryptionService.decrypt(encryptedNumber)).thenReturn(decryptedNumber);

        // When
        String masked = cardService.getMaskedCardNumber(testCard);

        // Then
        assertEquals("**** **** **** 1111", masked);
    }

    @Test
    void validateSufficientFunds_WithSufficientBalance_ShouldNotThrow() {
        // Given
        testCard.setBalance(new BigDecimal("100.00"));
        BigDecimal amount = new BigDecimal("50.00");

        // When & Then
        assertDoesNotThrow(() -> cardService.validateSufficientFunds(testCard, amount));
    }

    @Test
    void validateSufficientFunds_WithInsufficientBalance_ShouldThrowInsufficientFundsException() {
        // Given
        testCard.setBalance(new BigDecimal("50.00"));
        BigDecimal amount = new BigDecimal("100.00");

        // When & Then
        assertThrows(InsufficientFundsException.class, () ->
                cardService.validateSufficientFunds(testCard, amount)
        );
    }

    @Test
    void requestCardBlock_WithActiveCard_ShouldCreateBlockRequest() {
        // Given
        String cardNumber = "4111111111111111";
        String encryptedNumber = "encrypted123";
        String reason = "Card lost";

        when(encryptionService.encrypt(cardNumber)).thenReturn(encryptedNumber);
        when(cardRepository.findByCardNumberAndUser(encryptedNumber, testUser))
                .thenReturn(Optional.of(testCard));
        when(blockRequestRepository.existsByCardCardNumberAndStatus(encryptedNumber, BlockRequestStatus.PENDING))
                .thenReturn(false);
        when(blockRequestRepository.save(any(BlockRequest.class))).thenAnswer(invocation -> {
            BlockRequest request = invocation.getArgument(0);
            request.setId(1L);
            return request;
        });
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When
        BlockRequest request = cardService.requestCardBlock(cardNumber, testUser, reason);

        // Then
        assertNotNull(request);
        assertEquals(BlockRequestStatus.PENDING, request.getStatus());
        assertEquals(reason, request.getReason());
        assertEquals(CardStatus.PENDING_BLOCK, testCard.getStatus());
    }

    @Test
    void requestCardBlock_WithExistingPendingRequest_ShouldThrowException() {
        // Given
        String cardNumber = "4111111111111111";
        String encryptedNumber = "encrypted123";
        String reason = "Card lost";

        when(encryptionService.encrypt(cardNumber)).thenReturn(encryptedNumber);
        when(cardRepository.findByCardNumberAndUser(encryptedNumber, testUser))
                .thenReturn(Optional.of(testCard));
        when(blockRequestRepository.existsByCardCardNumberAndStatus(encryptedNumber, BlockRequestStatus.PENDING))
                .thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                cardService.requestCardBlock(cardNumber, testUser, reason)
        );
    }
}