package com.example.bankcards.dto;

import com.example.bankcards.entity.BlockRequestStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlockRequestResponse {
    private Long id;
    private String cardNumber; // Зашифрованный номер
    private String cardMasked; // Маскированный номер
    private String owner;
    private Long userId;
    private String username;
    private LocalDateTime requestDate;
    private String reason;
    private BlockRequestStatus status;
    private LocalDateTime processedDate;
    private String processedByUsername;

    public BlockRequestResponse() {}

    public BlockRequestResponse(Long id, String cardNumber, String cardMasked, String owner,
                                Long userId, String username, LocalDateTime requestDate,
                                String reason, BlockRequestStatus status,
                                LocalDateTime processedDate, String processedByUsername) {
        this.id = id;
        this.cardNumber = cardNumber;
        this.cardMasked = cardMasked;
        this.owner = owner;
        this.userId = userId;
        this.username = username;
        this.requestDate = requestDate;
        this.reason = reason;
        this.status = status;
        this.processedDate = processedDate;
        this.processedByUsername = processedByUsername;
    }
}