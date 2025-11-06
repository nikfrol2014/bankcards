package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardService cardService;

    // Выполнить перевод между картами
    public Transaction transferBetweenCards(Card fromCard, Card toCard, BigDecimal amount, String description) {
        // Валидируем карты для транзакции
        cardService.validateCardForTransaction(fromCard);
        cardService.validateCardForTransaction(toCard);

        // Проверяем, что это не перевод на ту же карту
        if (fromCard.getCardNumber().equals(toCard.getCardNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same card");
        }

        // Проверяем достаточность средств
        cardService.validateSufficientFunds(fromCard, amount);

        // Проверяем, что сумма положительная
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Обновляем балансы
        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        // Сохраняем карты
        cardService.save(fromCard);
        cardService.save(toCard);

        // Создаем транзакцию
        Transaction transaction = new Transaction(fromCard, toCard, amount, description);
        return transactionRepository.save(transaction);
    }

    // Получить историю транзакций по карте
    public Page<Transaction> getCardTransactions(String cardNumber, Pageable pageable) {
        return transactionRepository.findByCardNumber(cardNumber, pageable);
    }

    // Получить отправленные транзакции
    public Page<Transaction> getSentTransactions(String cardNumber, Pageable pageable) {
        return transactionRepository.findByFromCardCardNumberOrderByTransactionDateDesc(cardNumber, pageable);
    }

    // Получить полученные транзакции
    public Page<Transaction> getReceivedTransactions(String cardNumber, Pageable pageable) {
        return transactionRepository.findByToCardCardNumberOrderByTransactionDateDesc(cardNumber, pageable);
    }

    // Найти транзакции за период
    public Page<Transaction> getTransactionsByPeriod(String cardNumber, java.time.LocalDateTime startDate,
                                                     java.time.LocalDateTime endDate, Pageable pageable) {
        return transactionRepository.findByCardNumberAndPeriod(cardNumber, startDate, endDate, pageable);
    }

    // Найти транзакцию по ID
    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));
    }
}