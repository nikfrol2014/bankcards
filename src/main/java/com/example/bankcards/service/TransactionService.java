package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardService cardService;

    // Выполнить перевод между картами
    public Transaction transferBetweenCards(Card fromCard, Card toCard, BigDecimal amount, String description) {
        // Проверяем, что карты активны
        if (!cardService.isCardActive(fromCard)) {
            throw new RuntimeException("Source card is not active");
        }
        if (!cardService.isCardActive(toCard)) {
            throw new RuntimeException("Destination card is not active");
        }

        // Проверяем достаточность средств
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // Проверяем, что сумма положительная
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
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
    public Page<Transaction> getCardTransactions(Card card, Pageable pageable) {
        return transactionRepository.findByCard(card, pageable);
    }

    // Получить отправленные транзакции
    public Page<Transaction> getSentTransactions(Card card, Pageable pageable) {
        return transactionRepository.findByFromCardOrderByTransactionDateDesc(card, pageable);
    }

    // Получить полученные транзакции
    public Page<Transaction> getReceivedTransactions(Card card, Pageable pageable) {
        return transactionRepository.findByToCardOrderByTransactionDateDesc(card, pageable);
    }

    // Найти транзакции за период
    public Page<Transaction> getTransactionsByPeriod(Card card, LocalDateTime startDate,
                                                     LocalDateTime endDate, Pageable pageable) {
        return transactionRepository.findByCardAndPeriod(card, startDate, endDate, pageable);
    }

    // Найти транзакцию по ID
    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }
}
