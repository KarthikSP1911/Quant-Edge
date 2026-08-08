package com.quantedge.backend.service;

import com.quantedge.backend.dto.response.TimeMachineDecisionResponse;
import com.quantedge.backend.dto.response.TimeMachineHoldingResponse;
import com.quantedge.backend.dto.response.TimeMachineResultResponse;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.exception.InvalidTimeMachineRequestException;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.repository.OrderExecutionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Reconstructs a user's portfolio as it stood on a past date, by replaying every {@link
 * OrderExecution} up to that date ({@link TransactionReplayer}) and pricing the resulting
 * holdings with {@link HistoricalPriceService}. Best/worst decisions rank realized gain % - see
 * CLAUDE.md's Phase 4 Time Machine section for why that metric was chosen over realized $ or
 * avoided-loss framings.
 */
@Service
@RequiredArgsConstructor
public class TimeMachineService {

    /** Matches {@code User.balance}'s default - there is no deposit/withdrawal feature, so every
     * user's starting cash is this constant. */
    private static final BigDecimal STARTING_BALANCE = new BigDecimal("10000.00");

    private static final int MAX_DECISIONS = 5;

    private final OrderExecutionRepository orderExecutionRepository;
    private final HistoricalPriceService historicalPriceService;
    private final CompanyMapper companyMapper;

    public TimeMachineResultResponse getTimeMachine(User user, String asOfDate) {
        LocalDate date = parseDate(asOfDate);
        Instant asOf = date.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();

        List<OrderExecution> executions =
                orderExecutionRepository.findByOrderUserAndExecutedAtLessThanEqualOrderByExecutedAtAsc(user, asOf);
        TransactionReplayer.ReplayResult replay = TransactionReplayer.replay(executions);

        List<TimeMachineHoldingResponse> holdings = replay.openHoldings().stream()
                .map(holding -> toHoldingResponse(holding, date))
                .toList();

        BigDecimal totalMarketValue =
                holdings.stream().map(TimeMachineHoldingResponse::marketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cashBalance = STARTING_BALANCE.add(replay.cashDelta()).setScale(2, RoundingMode.HALF_UP);

        List<TimeMachineDecisionResponse> decisions = replay.realizedDecisions().stream()
                .map(this::toDecisionResponse)
                .toList();
        List<TimeMachineDecisionResponse> bestDecisions = decisions.stream()
                .sorted(Comparator.comparing(TimeMachineDecisionResponse::realizedGainPercent)
                        .reversed())
                .limit(MAX_DECISIONS)
                .toList();
        List<TimeMachineDecisionResponse> worstDecisions = decisions.stream()
                .sorted(Comparator.comparing(TimeMachineDecisionResponse::realizedGainPercent))
                .limit(MAX_DECISIONS)
                .toList();

        return new TimeMachineResultResponse(
                date.toString(),
                cashBalance,
                holdings,
                totalMarketValue,
                cashBalance.add(totalMarketValue),
                bestDecisions,
                worstDecisions);
    }

    private TimeMachineHoldingResponse toHoldingResponse(TransactionReplayer.HoldingState holding, LocalDate date) {
        BigDecimal priceAtDate = historicalPriceService
                .getClosePriceAsOf(holding.company().getSymbol(), date)
                .orElse(holding.averageCost());

        BigDecimal quantity = BigDecimal.valueOf(holding.quantity());
        BigDecimal marketValue = priceAtDate.multiply(quantity);
        BigDecimal costBasis = holding.averageCost().multiply(quantity);
        BigDecimal gainLoss = marketValue.subtract(costBasis);
        BigDecimal gainLossPercent = costBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : gainLoss.divide(costBasis, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return new TimeMachineHoldingResponse(
                companyMapper.toResponse(holding.company()),
                holding.quantity(),
                holding.averageCost(),
                priceAtDate,
                marketValue,
                gainLoss,
                gainLossPercent);
    }

    private TimeMachineDecisionResponse toDecisionResponse(TransactionReplayer.RealizedDecision decision) {
        return new TimeMachineDecisionResponse(
                decision.company().getSymbol(),
                decision.quantity(),
                decision.buyPrice(),
                decision.sellPrice(),
                decision.executedAt().toString(),
                decision.realizedGainPercent());
    }

    private LocalDate parseDate(String asOfDate) {
        try {
            LocalDate date = LocalDate.parse(asOfDate);
            if (date.isAfter(LocalDate.now())) {
                throw new InvalidTimeMachineRequestException("asOfDate cannot be in the future: " + asOfDate);
            }
            return date;
        } catch (DateTimeException ex) {
            throw new InvalidTimeMachineRequestException("asOfDate must be an ISO date (yyyy-MM-dd): " + asOfDate);
        }
    }
}
