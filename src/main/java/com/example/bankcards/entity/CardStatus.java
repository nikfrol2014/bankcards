package com.example.bankcards.entity;

public enum CardStatus {
    ACTIVE,           // Активна
    BLOCKED,          // Заблокирована
    EXPIRED,          // Истек срок
    PENDING_BLOCK     // Ожидает блокировки (новый статус)
}