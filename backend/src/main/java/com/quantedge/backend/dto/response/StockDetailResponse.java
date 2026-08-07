package com.quantedge.backend.dto.response;

import java.util.List;

public record StockDetailResponse(CompanyResponse company, QuoteResponse quote, List<CandleResponse> candles) {}
