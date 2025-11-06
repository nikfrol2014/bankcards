package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Найти все транзакции по карте (отправленные и полученные)
    @Query("SELECT t FROM Transaction t WHERE t.fromCard.cardNumber = :cardNumber OR t.toCard.cardNumber = :cardNumber ORDER BY t.transactionDate DESC")
    Page<Transaction> findByCardNumber(@Param("cardNumber") String cardNumber, Pageable pageable);

    // Найти отправленные транзакции по карте
    Page<Transaction> findByFromCardCardNumberOrderByTransactionDateDesc(String fromCardNumber, Pageable pageable);

    // Найти полученные транзакции по карте
    Page<Transaction> findByToCardCardNumberOrderByTransactionDateDesc(String toCardNumber, Pageable pageable);

    // Найти транзакции за период
    @Query("SELECT t FROM Transaction t WHERE (t.fromCard.cardNumber = :cardNumber OR t.toCard.cardNumber = :cardNumber) " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    Page<Transaction> findByCardNumberAndPeriod(@Param("cardNumber") String cardNumber,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);

    // Проверить существование транзакций по карте
    boolean existsByFromCardCardNumberOrToCardCardNumber(String fromCardNumber, String toCardNumber);
}
