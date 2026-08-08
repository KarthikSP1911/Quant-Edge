package com.quantedge.backend.config;

import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.dto.response.DashboardResponse;
import com.quantedge.backend.dto.response.OrderSummaryResponse;
import com.quantedge.backend.dto.response.PlacedOrderResponse;
import com.quantedge.backend.dto.response.StockDetailResponse;
import com.quantedge.backend.dto.response.WatchlistItemResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.service.DashboardService;
import com.quantedge.backend.service.OrderService;
import com.quantedge.backend.service.QuoteService;
import com.quantedge.backend.service.StockDetailService;
import com.quantedge.backend.service.WatchlistService;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class ChatToolsConfig {

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User user) {
            return user;
        }
        throw new IllegalStateException("No authenticated user found for tool execution");
    }

    public record SymbolRequest(String symbol) {}

    public record EmptyRequest() {}

    public record CancelOrderRequest(UUID orderId) {}

    public record CustomQuoteResponse(
            double currentPrice, double high, double low, double open, double previousClose, long timestamp) {}

    @Bean
    @Description("Get fundamentals, static profile, and historical chart data for a stock symbol")
    public Function<SymbolRequest, StockDetailResponse> getCompanyInfo(StockDetailService stockDetailService) {
        return request ->
                stockDetailService.getStockDetail(request.symbol(), "1day", 14).orElse(null);
    }

    @Bean
    @Description("Get the current real-time quote (price) for a stock symbol")
    public Function<SymbolRequest, CustomQuoteResponse> getQuote(QuoteService quoteService) {
        return request -> {
            FinnhubQuoteResponse quote = quoteService.getQuote(request.symbol());
            return new CustomQuoteResponse(
                    quote.currentPrice(),
                    quote.high(),
                    quote.low(),
                    quote.open(),
                    quote.previousClose(),
                    quote.timestamp());
        };
    }

    @Bean
    @Description("Get the user's dashboard, containing their portfolio performance and overall balance")
    public Function<EmptyRequest, DashboardResponse> getDashboard(DashboardService dashboardService) {
        return request -> dashboardService.getDashboard(getCurrentUser());
    }

    @Bean
    @Description("Get the user's current watchlist of stocks")
    public Function<EmptyRequest, List<WatchlistItemResponse>> getWatchlist(WatchlistService watchlistService) {
        return request -> watchlistService.list(getCurrentUser());
    }

    @Bean
    @Description("Add a stock symbol to the user's watchlist")
    public Function<SymbolRequest, List<WatchlistItemResponse>> addToWatchlist(WatchlistService watchlistService) {
        return request -> {
            watchlistService.add(getCurrentUser(), request.symbol());
            return watchlistService.list(getCurrentUser());
        };
    }

    @Bean
    @Description("Remove a stock symbol from the user's watchlist")
    public Function<SymbolRequest, List<WatchlistItemResponse>> removeFromWatchlist(WatchlistService watchlistService) {
        return request -> {
            watchlistService.remove(getCurrentUser(), request.symbol());
            return watchlistService.list(getCurrentUser());
        };
    }

    @Bean
    @Description("Get a list of all orders placed by the user")
    public Function<EmptyRequest, List<OrderSummaryResponse>> getUserOrders(OrderService orderService) {
        return request -> orderService.getOrderHistory(getCurrentUser());
    }

    @Bean
    @Description(
            "Place a new market or limit order for a stock. Type must be MARKET, LIMIT, STOP_LOSS, or STOP_LIMIT. Side must be BUY or SELL.")
    public Function<PlaceOrderRequest, Object> placeOrder(OrderService orderService) {
        return request -> {
            if ("MARKET".equals(request.getType().name())) {
                if ("BUY".equals(request.getSide().name())) {
                    return orderService.buy(getCurrentUser(), request.getSymbol(), request.getQuantity());
                } else {
                    return orderService.sell(getCurrentUser(), request.getSymbol(), request.getQuantity());
                }
            } else {
                return orderService.placeOrder(getCurrentUser(), request);
            }
        };
    }

    @Bean
    @Description("Cancel an existing open order")
    public Function<CancelOrderRequest, PlacedOrderResponse> cancelOrder(OrderService orderService) {
        return request -> orderService.cancelOrder(getCurrentUser(), request.orderId());
    }
}
