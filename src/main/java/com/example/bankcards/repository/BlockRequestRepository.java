package com.example.bankcards.repository;

import com.example.bankcards.entity.BlockRequest;
import com.example.bankcards.entity.BlockRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRequestRepository extends JpaRepository<BlockRequest, Long> {

    // Найти все pending запросы
    Page<BlockRequest> findByStatus(BlockRequestStatus status, Pageable pageable);

    // Найти запросы по карте
    List<BlockRequest> findByCardCardNumber(String cardNumber);

    // Найти запросы по пользователю
    Page<BlockRequest> findByUserUsername(String username, Pageable pageable);

    // Проверить существование pending запроса для карты
    boolean existsByCardCardNumberAndStatus(String cardNumber, BlockRequestStatus status);
}