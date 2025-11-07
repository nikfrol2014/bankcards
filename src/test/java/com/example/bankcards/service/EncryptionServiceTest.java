package com.example.bankcards.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "secretKey", "my16bytekey12345");
    }

    @Test
    void encryptAndDecrypt_ShouldWorkCorrectly() {
        // Given
        String originalText = "4111111111111111";

        // When
        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void encrypt_DifferentCardNumbers_ShouldProduceDifferentResults() {
        // Given
        String card1 = "4111111111111111";
        String card2 = "4222222222222222";

        // When
        String encrypted1 = encryptionService.encrypt(card1);
        String encrypted2 = encryptionService.encrypt(card2);

        // Then
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void decrypt_WithInvalidData_ShouldThrowException() {
        // Given
        String invalidEncryptedData = "invalid_encrypted_data";

        // When & Then
        assertThrows(RuntimeException.class, () ->
                encryptionService.decrypt(invalidEncryptedData)
        );
    }
}