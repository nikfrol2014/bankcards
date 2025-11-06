package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardResponse {
    private String maskedNumber;    // ТОЛЬКО маскированный номер
    private String owner;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
    private Long userId;

    public CardResponse() {}

    public CardResponse(String maskedNumber, String owner, LocalDate expiryDate,
                        CardStatus status, BigDecimal balance, Long userId) {
        this.maskedNumber = maskedNumber;
        this.owner = owner;
        this.expiryDate = expiryDate;
        this.status = status;
        this.balance = balance;
        this.userId = userId;
    }
}