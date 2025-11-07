package com.example.bankcards.entity;

// Статусы запросов на блокировку
public enum BlockRequestStatus {
    PENDING,    // Ожидает рассмотрения
    APPROVED,   // Одобрено
    REJECTED    // Отклонено
}
