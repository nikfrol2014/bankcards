package com.example.bankcards.controller;

import com.example.bankcards.entity.*;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.service.EncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print; // ДОБАВИЛИ
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    private Card createTestCard() {
        Card card = new Card();
        card.setCardNumber("encrypted-card-number");
        card.setOwner("TEST USER");
        card.setExpiryDate(LocalDate.now().plusYears(2));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(new BigDecimal("1000.00"));
        card.setUser(createTestUser());
        return card;
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getUserCards_Success() throws Exception {
        // Given
        User user = createTestUser();
        Card card = createTestCard();
        Page<Card> cardPage = new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1); // ИСПРАВИЛИ: добавили PageRequest

        when(userService.findByUsername("testuser")).thenReturn(user);
        when(cardService.getUserCards(eq(user), any(Pageable.class))).thenReturn(cardPage); // УТОЧНИЛИ тип
        when(cardService.getMaskedCardNumber(any(Card.class))).thenReturn("**** **** **** 1234");

        // When & Then
        mockMvc.perform(get("/api/cards")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "owner"))
                .andDo(print()) // для отладки
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].maskedNumber").value("**** **** **** 1234"))
                .andExpect(jsonPath("$.content[0].owner").value("TEST USER"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getCardBalance_Success() throws Exception {
        // Given
        User user = createTestUser();
        Card card = createTestCard();

        when(userService.findByUsername("testuser")).thenReturn(user);
        when(encryptionService.encrypt("4111111111111111")).thenReturn("encrypted-card");
        when(cardService.getByCardNumberAndUser("encrypted-card", user)).thenReturn(card);

        // When & Then
        mockMvc.perform(get("/api/cards/balance")
                        .param("cardNumber", "4111111111111111"))
                .andDo(print()) // ДОБАВИЛИ для отладки
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void activateCard_Success() throws Exception {
        // Given
        User user = createTestUser();
        Card card = createTestCard();
        card.setStatus(CardStatus.ACTIVE);

        when(userService.findByUsername("testuser")).thenReturn(user);
        when(encryptionService.encrypt("4111111111111111")).thenReturn("encrypted-card");
        when(cardService.updateCardStatus(eq("encrypted-card"), eq(CardStatus.ACTIVE), eq(user))).thenReturn(card);
        when(cardService.getMaskedCardNumber(any(Card.class))).thenReturn("**** **** **** 1234");

        // When & Then
        mockMvc.perform(put("/api/cards/activate")
                        .param("cardNumber", "4111111111111111")
                        .with(csrf()))
                .andDo(print()) // для отладки
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 1234"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void requestCardBlock_Success() throws Exception {
        // Given
        User user = createTestUser();
        Card card = createTestCard();

        // Создаем BlockRequest объект
        BlockRequest blockRequest = new BlockRequest();
        blockRequest.setId(1L);
        blockRequest.setCard(card);
        blockRequest.setUser(user);
        blockRequest.setReason("Card lost");
        blockRequest.setStatus(BlockRequestStatus.PENDING);
        blockRequest.setRequestDate(LocalDateTime.now());

        when(userService.findByUsername("testuser")).thenReturn(user);
        when(encryptionService.encrypt("4111111111111111")).thenReturn("encrypted-card");
        when(cardService.getByCardNumberAndUser("encrypted-card", user)).thenReturn(card);

        // правильно замокать requestCardBlock
        when(cardService.requestCardBlock(eq("4111111111111111"), eq(user), anyString()))
                .thenReturn(blockRequest);

        // When & Then
        mockMvc.perform(post("/api/cards/block-request")
                        .param("cardNumber", "4111111111111111")
                        .param("reason", "Card lost")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reason").value("Card lost"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}