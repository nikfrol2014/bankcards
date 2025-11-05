package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardResponse {
    private Long id;
    private String cardNumber; // Замаскированный номер
    private String owner;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
    private Long userId;


    public CardResponse() {}
    public CardResponse(Long id, String cardNumber, String owner, LocalDate expiryDate,
                        CardStatus status, BigDecimal balance, Long userId) {
        this.id = id;
        this.cardNumber = cardNumber;
        this.owner = owner;
        this.expiryDate = expiryDate;
        this.status = status;
        this.balance = balance;
        this.userId = userId;
    }
}
