package com.quantedge.backend.config;

import java.util.List;
import java.util.UUID;

import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.rag.retrieval.KnowledgeBaseService;
import com.quantedge.backend.rag.retrieval.KnowledgeChunkResult;
import com.quantedge.backend.service.DashboardService;
import com.quantedge.backend.service.OrderService;
import com.quantedge.backend.service.PendingOrderService;
import com.quantedge.backend.service.QuoteService;
import com.quantedge.backend.service.StockDetailService;
import com.quantedge.backend.service.WatchlistService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The chat agent's tool functions, exposed to {@link org.springframework.ai.chat.client.ChatClient}
 * via Spring AI's method-based tool calling ({@code @Tool}-annotated instance methods on a single
 * bean, passed to {@code ChatClient.prompt().tools(chatTools)}) rather than the older per-tool
 * {@code @Bean Function<Req,Resp>} style, which Spring AI 2.0 removed.
 */
@Component
public class ChatTools {

    private final StockDetailService stockDetailService;
    private final QuoteService quoteService;
    private final DashboardService dashboardService;
    private final WatchlistService watchlistService;
    private final OrderService orderService;
    private final PendingOrderService pendingOrderService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorStore knowledgeBaseVectorStore;

    public ChatTools(
            StockDetailService stockDetailService,
            QuoteService quoteService,
            DashboardService dashboardService,
            WatchlistService watchlistService,
            OrderService orderService,
            PendingOrderService pendingOrderService,
            KnowledgeBaseService knowledgeBaseService,
            @Lazy VectorStore knowledgeBaseVectorStore) {
        this.stockDetailService = stockDetailService;
        this.quoteService = quoteService;
        this.dashboardService = dashboardService;
        this.watchlistService = watchlistService;
        this.orderService = orderService;
        this.pendingOrderService = pendingOrderService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeBaseVectorStore = knowledgeBaseVectorStore;
    }

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User user) {
            return user;
        }
        throw new IllegalStateException("No authenticated user found for tool execution");
    }

    public record CustomQuoteResponse(
            double currentPrice, double high, double low, double open, double previousClose, long timestamp) {}

    @Tool(description = "Get fundamentals, static profile, and historical chart data for a stock symbol")
    public Object getCompanyInfo(@ToolParam(description = "Stock ticker symbol, e.g. AAPL") String symbol) {
        try {
            return stockDetailService.getStockDetail(symbol, "1day", 14).orElse(null);
        } catch (Exception e) {
            return "Error executing getCompanyInfo: " + e.getMessage();
        }
    }

    @Tool(description = "Get the current real-time quote (price) for a stock symbol")
    public Object getQuote(@ToolParam(description = "Stock ticker symbol, e.g. AAPL") String symbol) {
        try {
            FinnhubQuoteResponse quote = quoteService.getQuote(symbol);
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
    }

    @Tool(description = "Get the user's dashboard, containing their portfolio performance and overall balance")
    public Object getDashboard() {
        try {
            return dashboardService.getDashboard(getCurrentUser());
        } catch (Exception e) {
            return "Error executing getDashboard: " + e.getMessage();
        }
    }

    @Tool(description = "Get the user's current watchlist of stocks")
    public Object getWatchlist() {
        try {
            return watchlistService.list(getCurrentUser());
        } catch (Exception e) {
            return "Error executing getWatchlist: " + e.getMessage();
        }
    }

    @Tool(description = "Add a stock symbol to the user's watchlist")
    public Object addToWatchlist(@ToolParam(description = "Stock ticker symbol, e.g. AAPL") String symbol) {
        try {
            watchlistService.add(getCurrentUser(), symbol);
            return watchlistService.list(getCurrentUser());
        } catch (Exception e) {
            return "Error executing addToWatchlist: " + e.getMessage();
        }
    }

    @Tool(description = "Remove a stock symbol from the user's watchlist")
    public Object removeFromWatchlist(@ToolParam(description = "Stock ticker symbol, e.g. AAPL") String symbol) {
        try {
            watchlistService.remove(getCurrentUser(), symbol);
            return watchlistService.list(getCurrentUser());
        } catch (Exception e) {
            return "Error executing removeFromWatchlist: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all orders placed by the user")
    public Object getUserOrders() {
        try {
            return orderService.getOrderHistory(getCurrentUser());
        } catch (Exception e) {
            return "Error executing getUserOrders: " + e.getMessage();
        }
    }

    @Tool(
            description = "Propose a new market or limit order for a stock. Type must be MARKET, LIMIT, STOP_LOSS, or "
                    + "STOP_LIMIT. Side must be BUY or SELL. This never executes the order - it only stages a "
                    + "proposal for the user to review. The user must accept or reject it themselves via the "
                    + "confirmation card shown in the UI; you have no way to execute or discard it yourself, so "
                    + "do not claim you have placed the trade and do not ask the user to reply 'yes'/'no' in "
                    + "chat - just tell them a confirmation card is ready for them to act on.")
    public Object placeOrder(PlaceOrderRequest request) {
        try {
            User user = getCurrentUser();
            pendingOrderService.stage(user.getId(), request);
            return "Order proposed: " + request.getSide() + " " + request.getQuantity() + " of " + request.getSymbol()
                    + ". Waiting for the user to accept or reject it in the confirmation card.";
        } catch (Exception e) {
            return "Error staging order: " + e.getMessage();
        }
    }

    @Tool(description = "Cancel an existing open order in the market (not a pending staging order).")
    public Object cancelOrder(@ToolParam(description = "The order's UUID") UUID orderId) {
        try {
            return orderService.cancelOrder(getCurrentUser(), orderId);
        } catch (Exception e) {
            return "Error executing cancelOrder: " + e.getMessage();
        }
    }

    @Tool(
            description = "Search the RAG knowledge base of ingested news articles (refreshed daily from Finnhub) and "
                    + "research notes for context relevant to a question, so answers can be grounded in retrieved "
                    + "sources instead of the model's own memory. Returns the top matching chunks, ranked to favor "
                    + "recent news, each with its source title, symbol, source/url, and publishedAt timestamp when "
                    + "known.")
    public Object queryKnowledgeBase(@ToolParam(description = "Natural-language question to search for") String query) {
        try {
            List<KnowledgeChunkResult> results = knowledgeBaseService.denseSearch(knowledgeBaseVectorStore, query, 5);
            return results;
        } catch (Exception e) {
            return "Error executing queryKnowledgeBase: " + e.getMessage();
        }
    }
}
