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
    @JoinColumn(name = "from_card_number", nullable = false) // Теперь ссылается на card_number
    private Card fromCard;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_card_number", nullable = false) // Теперь ссылается на card_number
    private Card toCard;

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(length = 500)
    private String description;

    public Transaction() {}

    public Transaction(Card fromCard, Card toCard, BigDecimal amount, String description) {
        this.fromCard = fromCard;
        this.toCard = toCard;
        this.amount = amount;
        this.transactionDate = LocalDateTime.now();
        this.description = description;
    }
}