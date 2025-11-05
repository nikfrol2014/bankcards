package com.example.bankcards.exception;

public class CardExpiredException extends RuntimeException {
    public CardExpiredException(String message) {
        super(message);
    }

    public CardExpiredException(Long cardId) {
        super("Card is expired: " + cardId);
    }
}
