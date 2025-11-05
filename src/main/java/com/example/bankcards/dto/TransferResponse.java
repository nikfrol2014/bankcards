package com.example.bankcards.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransferResponse {
    private Long id;
    private Long fromCardId;
    private Long toCardId;
    private String fromCardMasked;
    private String toCardMasked;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;


    public TransferResponse() {}
    public TransferResponse(Long id, Long fromCardId, Long toCardId, String fromCardMasked,
                            String toCardMasked, BigDecimal amount, LocalDateTime transactionDate,
                            String description) {
        this.id = id;
        this.fromCardId = fromCardId;
        this.toCardId = toCardId;
        this.fromCardMasked = fromCardMasked;
        this.toCardMasked = toCardMasked;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
    }
}
