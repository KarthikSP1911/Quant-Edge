package com.quantedge.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.quantedge.backend.dto.response.DashboardResponse;
import com.quantedge.backend.dto.response.PortfolioPositionResponse;
import com.quantedge.backend.dto.response.PortfolioSummaryResponse;
import com.quantedge.backend.dto.response.TransactionResponse;
import com.quantedge.backend.dto.response.WatchlistItemResponse;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.entity.Portfolio;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.repository.OrderExecutionRepository;
import com.quantedge.backend.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Aggregates the dashboard's three panels into a single read: portfolio positions valued at
 * current live quotes, a watchlist preview, and the most recent order executions (there is no
 * separate transactions table — market orders execute synchronously, so each OrderExecution is
 * already the completed transaction record).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int WATCHLIST_PREVIEW_SIZE = 5;

    private final PortfolioRepository portfolioRepository;
    private final OrderExecutionRepository orderExecutionRepository;
    private final WatchlistService watchlistService;
    private final QuoteService quoteService;
    private final CompanyMapper companyMapper;

    public DashboardResponse getDashboard(User user) {
        PortfolioSummaryResponse portfolio = getPortfolioSummary(user);
        List<WatchlistItemResponse> watchlistPreview = watchlistService.list(user).stream()
                .limit(WATCHLIST_PREVIEW_SIZE)
                .toList();
        List<TransactionResponse> recentTransactions =
                orderExecutionRepository.findTop10ByOrderUserOrderByExecutedAtDesc(user).stream()
                        .map(this::toTransactionResponse)
                        .toList();

        return new DashboardResponse(portfolio, watchlistPreview, recentTransactions);
    }

    public PortfolioSummaryResponse getPortfolioSummary(User user) {
        List<PortfolioPositionResponse> positions = portfolioRepository.findByUser(user).stream()
                .map(this::toPositionResponse)
                .toList();

        BigDecimal totalMarketValue =
                positions.stream().map(PortfolioPositionResponse::marketValue).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortfolioSummaryResponse(
                user.getBalance(),
                positions,
                totalMarketValue,
                user.getBalance().add(totalMarketValue));
    }

    private PortfolioPositionResponse toPositionResponse(Portfolio portfolio) {
        String symbol = portfolio.getCompany().getSymbol();
        FinnhubQuoteResponse quote = quoteService.getQuote(symbol);
        BigDecimal currentPrice = BigDecimal.valueOf(quote.currentPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal previousClose = BigDecimal.valueOf(quote.previousClose()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal changePercent = previousClose.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : currentPrice
                        .subtract(previousClose)
                        .divide(previousClose, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        BigDecimal quantity = BigDecimal.valueOf(portfolio.getQuantity());
        BigDecimal marketValue = currentPrice.multiply(quantity);
        BigDecimal costBasis = portfolio.getAverageCost().multiply(quantity);
        BigDecimal gainLoss = marketValue.subtract(costBasis);
        BigDecimal gainLossPercent = costBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : gainLoss.divide(costBasis, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return new PortfolioPositionResponse(
                companyMapper.toResponse(portfolio.getCompany()),
                portfolio.getQuantity(),
                portfolio.getAverageCost(),
                currentPrice,
                previousClose,
                changePercent,
                marketValue,
                gainLoss,
                gainLossPercent);
    }

    private TransactionResponse toTransactionResponse(OrderExecution execution) {
        return new TransactionResponse(
                execution.getId(),
                execution.getOrder().getCompany().getSymbol(),
                execution.getOrder().getSide(),
                execution.getExecutedQuantity(),
                execution.getExecutionPrice(),
                execution.getExecutedAt().toString());
    }
}
