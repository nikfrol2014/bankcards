package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "card_number", nullable = false, unique = true)
    private String cardNumber; // Зашифрованный номер

    @NotNull
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String owner; // Владелец карты

    @NotNull
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate; // Срок действия

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status; // Статус карты

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance; // Баланс

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Владелец карты

    public Card() {}

    public Card(String cardNumber, String owner, LocalDate expiryDate,
                CardStatus status, BigDecimal balance, User user) {
        this.cardNumber = cardNumber;
        this.owner = owner;
        this.expiryDate = expiryDate;
        this.status = status;
        this.balance = balance;
        this.user = user;
    }
}