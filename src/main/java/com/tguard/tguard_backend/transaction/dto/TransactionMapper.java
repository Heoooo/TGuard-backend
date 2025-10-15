package com.tguard.tguard_backend.transaction.dto;

import com.tguard.tguard_backend.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    /**
     * Transaction 엔티티를 TransactionResponse DTO로 변환합니다.
     */
    public TransactionResponse toResponse(Transaction t) {
        if (t == null) return null;
        return new TransactionResponse(
                t.getId(),
                t.getUser().getId(),
                t.getAmount(),
                t.getLocation(),
                t.getDeviceInfo(),
                t.getTransactionTime(),
                t.getStatus().name()
        );
    }
}