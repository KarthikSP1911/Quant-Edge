package com.quantedge.backend.resolver;

import java.util.List;

import com.quantedge.backend.dto.response.StockComparisonResponse;
import com.quantedge.backend.service.StockComparisonService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class StockComparisonResolver {

    private final StockComparisonService stockComparisonService;

    public StockComparisonResolver(StockComparisonService stockComparisonService) {
        this.stockComparisonService = stockComparisonService;
    }

    @QueryMapping
    public StockComparisonResponse stockComparison(
            @Argument List<String> symbols, @Argument String interval, @Argument int outputSize) {
        return stockComparisonService.compare(symbols, interval, outputSize);
    }
}
