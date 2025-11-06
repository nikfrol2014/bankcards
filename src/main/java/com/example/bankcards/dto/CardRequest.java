package com.example.bankcards.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardRequest {
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number must be 16 digits")
    private String cardNumber;

    @NotBlank(message = "Owner name is required")
    @Size(min = 2, max = 100, message = "Owner name must be between 2 and 100 characters")
    private String owner;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @DecimalMin(value = "0.00", message = "Balance must be positive")
    private BigDecimal balance; // Опционально - начальный баланс

    public CardRequest() {}

    public CardRequest(String cardNumber, String owner, LocalDate expiryDate, BigDecimal balance) {
        this.cardNumber = cardNumber;
        this.owner = owner;
        this.expiryDate = expiryDate;
        this.balance = balance;
    }
}
