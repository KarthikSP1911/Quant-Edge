package com.quantedge.backend.config;

import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.service.DashboardService;
import com.quantedge.backend.service.OrderService;
import com.quantedge.backend.service.QuoteService;
import com.quantedge.backend.service.StockDetailService;
import com.quantedge.backend.service.WatchlistService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class ChatToolsConfig {

    private final Map<UUID, PlaceOrderRequest> pendingOrders = new ConcurrentHashMap<>();

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
    public Function<SymbolRequest, Object> getCompanyInfo(StockDetailService stockDetailService) {
        return request -> {
            try {
                return stockDetailService
                        .getStockDetail(request.symbol(), "1day", 14)
                        .orElse(null);
            } catch (Exception e) {
                return "Error executing getCompanyInfo: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Get the current real-time quote (price) for a stock symbol")
    public Function<SymbolRequest, Object> getQuote(QuoteService quoteService) {
        return request -> {
            try {
                FinnhubQuoteResponse quote = quoteService.getQuote(request.symbol());
                return new CustomQuoteResponse(
                        quote.currentPrice(),
                        quote.high(),
                        quote.low(),
                        quote.open(),
                        quote.previousClose(),
                        quote.timestamp());
            } catch (Exception e) {
                return "Error executing getQuote: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Get the user's dashboard, containing their portfolio performance and overall balance")
    public Function<EmptyRequest, Object> getDashboard(DashboardService dashboardService) {
        return request -> {
            try {
                return dashboardService.getDashboard(getCurrentUser());
            } catch (Exception e) {
                return "Error executing getDashboard: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Get the user's current watchlist of stocks")
    public Function<EmptyRequest, Object> getWatchlist(WatchlistService watchlistService) {
        return request -> {
            try {
                return watchlistService.list(getCurrentUser());
            } catch (Exception e) {
                return "Error executing getWatchlist: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Add a stock symbol to the user's watchlist")
    public Function<SymbolRequest, Object> addToWatchlist(WatchlistService watchlistService) {
        return request -> {
            try {
                watchlistService.add(getCurrentUser(), request.symbol());
                return watchlistService.list(getCurrentUser());
            } catch (Exception e) {
                return "Error executing addToWatchlist: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Remove a stock symbol from the user's watchlist")
    public Function<SymbolRequest, Object> removeFromWatchlist(WatchlistService watchlistService) {
        return request -> {
            try {
                watchlistService.remove(getCurrentUser(), request.symbol());
                return watchlistService.list(getCurrentUser());
            } catch (Exception e) {
                return "Error executing removeFromWatchlist: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Get a list of all orders placed by the user")
    public Function<EmptyRequest, Object> getUserOrders(OrderService orderService) {
        return request -> {
            try {
                return orderService.getOrderHistory(getCurrentUser());
            } catch (Exception e) {
                return "Error executing getUserOrders: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description(
            "Stage a new market or limit order for a stock. Type must be MARKET, LIMIT, STOP_LOSS, or STOP_LIMIT. Side must be BUY or SELL. This does not execute the order; it waits for user confirmation.")
    public Function<PlaceOrderRequest, Object> placeOrder() {
        return request -> {
            try {
                User user = getCurrentUser();
                pendingOrders.put(user.getId(), request);
                return "Order staged for " + request.getSide() + " " + request.getQuantity() + " of "
                        + request.getSymbol()
                        + ". Please ask the user to confirm by saying 'yes' to proceed, or 'no' to cancel.";
            } catch (Exception e) {
                return "Error staging order: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Execute the pending order after the user has explicitly confirmed 'yes'.")
    public Function<EmptyRequest, Object> confirmPendingOrder(OrderService orderService) {
        return request -> {
            try {
                User user = getCurrentUser();
                PlaceOrderRequest pending = pendingOrders.remove(user.getId());
                if (pending == null) {
                    return "No pending order found to confirm.";
                }

                if ("MARKET".equals(pending.getType().name())) {
                    if ("BUY".equals(pending.getSide().name())) {
                        return orderService.buy(user, pending.getSymbol(), pending.getQuantity());
                    } else {
                        return orderService.sell(user, pending.getSymbol(), pending.getQuantity());
                    }
                } else {
                    return orderService.placeOrder(user, pending);
                }
            } catch (Exception e) {
                return "Error executing confirmed order: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Cancel an existing open order in the market (not a pending staging order).")
    public Function<CancelOrderRequest, Object> cancelOrder(OrderService orderService) {
        return request -> {
            try {
                return orderService.cancelOrder(getCurrentUser(), request.orderId());
            } catch (Exception e) {
                return "Error executing cancelOrder: " + e.getMessage();
            }
        };
    }
}
