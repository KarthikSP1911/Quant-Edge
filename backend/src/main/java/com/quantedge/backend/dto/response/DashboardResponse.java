package com.quantedge.backend.dto.response;

import java.util.List;

public record DashboardResponse(
        PortfolioSummaryResponse portfolio,
        List<WatchlistItemResponse> watchlistPreview,
        List<TransactionResponse> recentTransactions) {}
