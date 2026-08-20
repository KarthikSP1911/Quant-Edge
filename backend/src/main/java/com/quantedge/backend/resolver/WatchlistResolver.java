package com.quantedge.backend.resolver;

import java.util.List;

import com.quantedge.backend.dto.response.QuoteResponse;
import com.quantedge.backend.dto.response.WatchlistItemResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.mapper.MarketDataMapper;
import com.quantedge.backend.service.QuoteService;
import com.quantedge.backend.service.WatchlistService;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class WatchlistResolver {

    private final WatchlistService watchlistService;
    private final QuoteService quoteService;
    private final MarketDataMapper marketDataMapper;

    public WatchlistResolver(
            WatchlistService watchlistService, QuoteService quoteService, MarketDataMapper marketDataMapper) {
        this.watchlistService = watchlistService;
        this.quoteService = quoteService;
        this.marketDataMapper = marketDataMapper;
    }

    @QueryMapping
    public List<WatchlistItemResponse> watchlist(@AuthenticationPrincipal User user) {
        return watchlistService.list(user);
    }

    @SchemaMapping(typeName = "WatchlistItem", field = "quote")
    public QuoteResponse quote(WatchlistItemResponse item) {
        return marketDataMapper.toQuoteResponse(
                quoteService.getQuote(item.company().symbol()));
    }
}
