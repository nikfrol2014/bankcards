package com.example.bankcards.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(Long cardId, Double balance, Double amount) {
        super(String.format("Insufficient funds on card %d: balance=%.2f, required=%.2f",
                cardId, balance, amount));
    }
}