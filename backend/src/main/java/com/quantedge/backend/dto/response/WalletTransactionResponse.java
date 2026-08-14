package com.quantedge.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id, long amountUsdCents, BigDecimal creditsAwarded, String status, Instant createdAt) {}
