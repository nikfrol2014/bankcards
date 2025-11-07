package com.example.bankcards.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "encryption.secret-key=my16bytekey12345"
})
class EncryptionIntegrationTest {

    @Autowired
    private EncryptionService encryptionService;

    @Test
    void encryptionService_WithProductionKey_ShouldWorkCorrectly() {
        // Given - тестовые номера карт
        String[] testCards = {
                "4111111111111111",
                "4222222222222222",
                "4333333333333333"
        };

        for (String originalCard : testCards) {
            // When
            String encrypted = encryptionService.encrypt(originalCard);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then
            assertNotNull(encrypted);
            assertNotEquals(originalCard, encrypted);
            assertEquals(originalCard, decrypted);
        }
    }

    @Test
    void encryptionService_ShouldBeConsistent() {
        // Given
        String cardNumber = "4555555555555555";

        // When
        String encrypted1 = encryptionService.encrypt(cardNumber);
        String encrypted2 = encryptionService.encrypt(cardNumber);
        String decrypted1 = encryptionService.decrypt(encrypted1);
        String decrypted2 = encryptionService.decrypt(encrypted2);

        // Then - с одинаковым ключом результат должен быть одинаковым
        assertEquals(encrypted1, encrypted2);
        assertEquals(cardNumber, decrypted1);
        assertEquals(cardNumber, decrypted2);
    }
}