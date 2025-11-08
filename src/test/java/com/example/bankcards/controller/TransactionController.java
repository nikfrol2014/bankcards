package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.Role;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.service.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private CardService cardService;

    @MockBean
    private UserService userService;

    @MockBean
    private EncryptionService encryptionService;

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.ROLE_USER);
        return user;
    }

    private Card createTestCard(String cardNumber) {
        Card card = new Card();
        card.setCardNumber("encrypted-" + cardNumber);
        card.setOwner("TEST USER");
        card.setExpiryDate(LocalDate.now().plusYears(2));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(new BigDecimal("1000.00"));
        card.setUser(createTestUser());
        return card;
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void transferBetweenCards_Success() throws Exception {
        // Given
        User user = createTestUser();
        Card fromCard = createTestCard("4111111111111111");
        Card toCard = createTestCard("4222222222222222");
        Transaction transaction = new Transaction(fromCard, toCard, new BigDecimal("100.00"), "Test transfer");
        transaction.setId(1L);

        TransferRequest transferRequest = new TransferRequest(
                "4111111111111111",
                "4222222222222222",
                new BigDecimal("100.00"),
                "Test transfer"
        );

        when(userService.findByUsername("testuser")).thenReturn(user);
        when(encryptionService.encrypt("4111111111111111")).thenReturn("encrypted-4111");
        when(encryptionService.encrypt("4222222222222222")).thenReturn("encrypted-4222");
        when(cardService.getByCardNumberAndUser("encrypted-4111", user)).thenReturn(fromCard);
        when(cardService.getByCardNumberAndUser("encrypted-4222", user)).thenReturn(toCard);
        when(transactionService.transferBetweenCards(eq(fromCard), eq(toCard), any(BigDecimal.class), anyString()))
                .thenReturn(transaction);
        when(cardService.getMaskedCardNumber(fromCard)).thenReturn("**** **** **** 1111");
        when(cardService.getMaskedCardNumber(toCard)).thenReturn("**** **** **** 2222");

        // When & Then
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fromCardMasked").value("**** **** **** 1111"))
                .andExpect(jsonPath("$.toCardMasked").value("**** **** **** 2222"))
                .andExpect(jsonPath("$.amount").value(100.00));
    }
}