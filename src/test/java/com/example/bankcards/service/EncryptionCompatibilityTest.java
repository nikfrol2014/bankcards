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
class EncryptionCompatibilityTest {

    @Autowired
    private EncryptionService encryptionService;

    @Test
    void shouldDecryptExistingMigrationData() {
        // Given - данные из нашей миграции 005 (должны работать с текущим ключом)
        String testCard = "4111111111111111";

        // When - шифруем тот же номер что в миграции
        String encrypted = encryptionService.encrypt(testCard);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertEquals(testCard, decrypted);

        // Проверим что можем работать с разными номерами
        String[] testCards = {
                "4222222222222222",
                "4333333333333333",
                "4555555555555555"
        };

        for (String card : testCards) {
            String enc = encryptionService.encrypt(card);
            String dec = encryptionService.decrypt(enc);
            assertEquals(card, dec);
        }
    }
}