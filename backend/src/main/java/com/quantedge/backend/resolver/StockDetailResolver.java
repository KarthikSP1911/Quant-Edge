package com.quantedge.backend.resolver;

import com.quantedge.backend.dto.response.StockDetailResponse;
import com.quantedge.backend.service.StockDetailService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class StockDetailResolver {

    private final StockDetailService stockDetailService;

    public StockDetailResolver(StockDetailService stockDetailService) {
        this.stockDetailService = stockDetailService;
    }

    @QueryMapping
    public StockDetailResponse stockDetail(
            @Argument String symbol, @Argument String interval, @Argument int outputSize) {
        return stockDetailService.getStockDetail(symbol, interval, outputSize).orElse(null);
    }
}
