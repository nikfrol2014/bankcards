package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, String> { // ID теперь String!

    // Найти все карты пользователя с пагинацией
    Page<Card> findByUser(User user, Pageable pageable);

    // Найти все карты пользователя по статусу с пагинацией
    Page<Card> findByUserAndStatus(User user, CardStatus status, Pageable pageable);

    // Найти карту по номеру и пользователю (проверка владения)
    Optional<Card> findByCardNumberAndUser(String cardNumber, User user);

    // Проверить существование карты по номеру
    boolean existsByCardNumber(String cardNumber);

    // Найти все активные карты пользователя
    List<Card> findByUserAndStatus(User user, CardStatus status);

    // Поиск карт пользователя по владельцу (имя на карте)
    @Query("SELECT c FROM Card c WHERE c.user = :user AND LOWER(c.owner) LIKE LOWER(CONCAT('%', :owner, '%'))")
    Page<Card> findByUserAndOwnerContainingIgnoreCase(@Param("user") User user,
                                                      @Param("owner") String owner,
                                                      Pageable pageable);

    // Получить все карты (для админа)
    Page<Card> findAll(Pageable pageable);
}