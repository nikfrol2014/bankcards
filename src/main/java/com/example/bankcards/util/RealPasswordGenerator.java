package com.example.bankcards.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class RealPasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("=== REAL BCrypt HASHES FOR MIGRATION ===");
        System.out.println();

        // Админ
        String adminHash = encoder.encode("admin123");
        System.out.println("-- ADMIN --");
        System.out.println("Username: admin");
        System.out.println("Password: admin123");
        System.out.println("Hash: " + adminHash);
        System.out.println();

        // Пользователь 1
        String user1Hash = encoder.encode("user1123");
        System.out.println("-- USER 1 --");
        System.out.println("Username: user1");
        System.out.println("Password: user1123");
        System.out.println("Hash: " + user1Hash);
        System.out.println();

        // Пользователь 2
        String user2Hash = encoder.encode("user2123");
        System.out.println("-- USER 2 --");
        System.out.println("Username: user2");
        System.out.println("Password: user2123");
        System.out.println("Hash: " + user2Hash);
    }
}