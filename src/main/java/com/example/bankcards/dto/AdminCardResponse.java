package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AdminCardResponse {
    private String cardNumber;       // Зашифрованный номер (ID)
    private String decryptedNumber;  // Оригинальный номер
    private String maskedNumber;     // Замаскированный номер
    private String owner;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
    private Long userId;
    private String username;

    public AdminCardResponse() {}

    public AdminCardResponse(String cardNumber, String decryptedNumber, String maskedNumber, String owner,
                             LocalDate expiryDate, CardStatus status, BigDecimal balance, Long userId, String username) {
        this.cardNumber = cardNumber;
        this.decryptedNumber = decryptedNumber;
        this.maskedNumber = maskedNumber;
        this.owner = owner;
        this.expiryDate = expiryDate;
        this.status = status;
        this.balance = balance;
        this.userId = userId;
        this.username = username;
    }
}