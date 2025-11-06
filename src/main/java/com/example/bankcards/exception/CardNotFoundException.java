package com.example.bankcards.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String message) {
        super(message);
    }

    public CardNotFoundException(String maskedCardNumber, String reason) {
        super("Card not found: " + maskedCardNumber + " - " + reason);
    }
}