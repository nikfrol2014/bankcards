package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_card_id", nullable = false)
    private Card fromCard; // Карта отправителя

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_card_id", nullable = false)
    private Card toCard; // Карта получателя

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // Сумма перевода

    @NotNull
    @Column(nullable = false)
    private LocalDateTime transactionDate; // Дата и время перевода

    @Column(length = 500)
    private String description; // Описание перевода

    public Transaction() {}

    public Transaction(Card fromCard, Card toCard, BigDecimal amount,
                       String description) {
        this.fromCard = fromCard;
        this.toCard = toCard;
        this.amount = amount;
        this.transactionDate = LocalDateTime.now();
        this.description = description;
    }
}