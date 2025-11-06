package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.example.bankcards.service.EncryptionService;

@Component
public class CardDataMigrationUtil implements CommandLineRunner {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "migrate-cards".equals(args[0])) {
            System.out.println("=== CARD DATA MIGRATION UTILITY ===");
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

            System.out.println("-- SQL UPDATE QUERIES --");
            for (String number : testNumbers) {
                String encrypted = encryptionService.encrypt(number);
                System.out.println("UPDATE cards SET card_number = '" + encrypted +
                        "' WHERE card_number = 'encrypted_" + number + "';");
            }

            System.out.println();
            System.out.println("-- YAML FOR LIQUIBASE --");
            for (String number : testNumbers) {
                String encrypted = encryptionService.encrypt(number);
                System.out.println("- update:");
                System.out.println("    tableName: cards");
                System.out.println("    where: \"card_number = 'encrypted_" + number + "'\"");
                System.out.println("    columns:");
                System.out.println("      - column: {name: card_number, value: \"" + encrypted + "\"}");
            }
        }
    }
}