package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CardRequest {
    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotBlank(message = "Owner name is required")
    @Size(min = 2, max = 100, message = "Owner name must be between 2 and 100 characters")
    private String owner;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;


    public CardRequest() {}
    public CardRequest(String cardNumber, String owner, LocalDate expiryDate) {
        this.cardNumber = cardNumber;
        this.owner = owner;
        this.expiryDate = expiryDate;
    }
}
