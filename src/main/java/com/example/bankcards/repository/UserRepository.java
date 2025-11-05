package com.example.bankcards.repository;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Найти пользователя по username
    Optional<User> findByUsername(String username);

    // Найти пользователя по email
    Optional<User> findByEmail(String email);

    // Найти пользователя по ID
    Optional<User> findById(Long id);

    // Проверить существование пользователя по username
    boolean existsByUsername(String username);

    // Проверить существование пользователя по email
    boolean existsByEmail(String email);

    // Поиск по username (частичное совпадение)
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    // Количество пользователей по роли
    long countByRole(Role role);
}
