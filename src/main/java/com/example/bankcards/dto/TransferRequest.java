package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotNull(message = "From card number is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "From card number must be 16 digits")
    private String fromCardNumber; // Оригинальный номер

    @NotNull(message = "To card number is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "To card number must be 16 digits")
    private String toCardNumber; // Оригинальный номер

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;

    public TransferRequest() {}

    public TransferRequest(String fromCardNumber, String toCardNumber, BigDecimal amount, String description) {
        this.fromCardNumber = fromCardNumber;
        this.toCardNumber = toCardNumber;
        this.amount = amount;
        this.description = description;
    }
}