package com.example.bankcards.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionUnitTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "secretKey", "my16bytekey12345");
    }

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        // Given
        String[] testCards = {
                "4111111111111111",
                "4222222222222222",
                "4333333333333333",
                "4555555555555555",
                "4666666666666666"
        };

        for (String card : testCards) {
            // When
            String encrypted = encryptionService.encrypt(card);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then
            assertNotNull(encrypted, "Encryption failed for: " + card);
            assertNotEquals(card, encrypted, "Encryption didn't change the value for: " + card);
            assertEquals(card, decrypted, "Decryption failed for: " + card);
            assertTrue(encrypted.length() > 20, "Encrypted value too short for: " + card);
        }
    }

    @Test
    void shouldHandleEdgeCases() {
        // Given
        String[] edgeCases = {
                "0000000000000000",
                "9999999999999999",
                "1234567812345678"
        };

        for (String card : edgeCases) {
            // When
            String encrypted = encryptionService.encrypt(card);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then
            assertEquals(card, decrypted, "Failed for edge case: " + card);
        }
    }

    @Test
    void shouldThrowExceptionOnInvalidEncryptedData() {
        // Given
        String invalidData = "not-valid-base64-data";

        // When & Then
        assertThrows(RuntimeException.class, () ->
                encryptionService.decrypt(invalidData)
        );
    }
}