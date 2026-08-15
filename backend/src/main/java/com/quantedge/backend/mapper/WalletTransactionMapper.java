package com.quantedge.backend.mapper;

import com.quantedge.backend.dto.response.WalletTransactionResponse;
import com.quantedge.backend.entity.WalletTransaction;
import org.springframework.stereotype.Component;

@Component
public class WalletTransactionMapper {

    public WalletTransactionResponse toResponse(WalletTransaction transaction) {
        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getAmountUsdCents(),
                transaction.getCreditsAwarded(),
                transaction.getStatus().name(),
                transaction.getCreatedAt());
    }
}
