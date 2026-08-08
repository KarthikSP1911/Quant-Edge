package com.quantedge.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record TimeMachineResultResponse(
        String asOfDate,
        BigDecimal cashBalance,
        List<TimeMachineHoldingResponse> holdings,
        BigDecimal totalMarketValue,
        BigDecimal totalAccountValue,
        List<TimeMachineDecisionResponse> bestDecisions,
        List<TimeMachineDecisionResponse> worstDecisions) {}
