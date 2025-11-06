package com.example.bankcards.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String maskedCardNumber, Double balance, Double amount) {
        super(String.format("Insufficient funds on card %s: balance=%.2f, required=%.2f",
                maskedCardNumber, balance, amount));
    }
}