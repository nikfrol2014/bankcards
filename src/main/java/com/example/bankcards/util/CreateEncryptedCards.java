package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CreateEncryptedCards {

    private static final String SECRET_KEY = "my16bytekey12345"; // совпадать с application.yml
    private static final String ALGORITHM = "AES";

    private static byte[] getValidKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        byte[] validKey = new byte[16];
        System.arraycopy(keyBytes, 0, validKey, 0, Math.min(keyBytes.length, 16));
        return validKey;
    }

    private static String encrypt(String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getValidKey(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data: " + data, e);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== REAL ENCRYPTED CARD NUMBERS FOR MIGRATION ===");
        System.out.println();

        String[] testNumbers = {
                "4111111111111111",
                "4222222222222222",
                "4333333333333333",
                "4555555555555555",
                "4666666666666666",
                "4777777777777777",
                "4888888888888888"
        };

        for (String number : testNumbers) {
            String encrypted = encrypt(number);
            System.out.println("-- Card: " + number + " --");
            System.out.println("Encrypted: " + encrypted);

            // SQL для миграции
            System.out.println("SQL: UPDATE cards SET card_number = '" + encrypted + "' WHERE card_number LIKE '%" + number.substring(12) + "';");
            System.out.println();
        }

        System.out.println("=== YAML FOR MIGRATION ===");
        System.out.println();

        for (int i = 0; i < testNumbers.length; i++) {
            String encrypted = encrypt(testNumbers[i]);
            System.out.println("# Card " + (i + 1) + ": " + testNumbers[i]);
            System.out.println("- update:");
            System.out.println("    tableName: cards");
            System.out.println("    where: \"card_number like '%encrypted_" + testNumbers[i] + "'\"");
            System.out.println("    columns:");
            System.out.println("      - column: {name: card_number, value: \"" + encrypted + "\"}");
            System.out.println();
        }
    }
}