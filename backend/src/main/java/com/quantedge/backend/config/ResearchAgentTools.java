package com.quantedge.backend.config;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.quantedge.backend.cache.IndicatorCache;
import com.quantedge.backend.cache.NewsCache;
import com.quantedge.backend.external.AlphaVantageClient;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.dto.FinnhubNewsResponse;
import com.quantedge.backend.rag.retrieval.KnowledgeBaseService;
import com.quantedge.backend.rag.retrieval.KnowledgeChunkResult;
import com.quantedge.backend.service.StockDetailService;
import com.quantedge.backend.service.agent.RetryingToolExecutor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * The research agent's tool surface. This is deliberately a separate bean from {@link ChatTools}:
 * every method here is read-only, so wiring the research orchestrator's {@code ChatClient} to
 * this bean alone (never {@code ChatTools}) is what enforces "the research agent cannot place,
 * confirm, or cancel a trade or touch the watchlist" - a tool permission boundary enforced by
 * Java wiring, not by a prompt instruction the model could ignore or a user could talk it out of.
 *
 * <p>Each method routes through {@link RetryingToolExecutor} so a flaky/rate-limited upstream
 * (Finnhub, Alpha Vantage) is retried and time-bounded before its failure is handed back to the
 * model as an observation for it to react to - by trying an alternative tool, proceeding with a
 * noted caveat, or asking a follow-up - rather than crashing the run.
 */
@Component
public class ResearchAgentTools {

    private final StockDetailService stockDetailService;
    private final FinnhubClient finnhubClient;
    private final AlphaVantageClient alphaVantageClient;
    private final NewsCache newsCache;
    private final IndicatorCache indicatorCache;
    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorStore knowledgeBaseVectorStore;
    private final RetryingToolExecutor retryingToolExecutor;

    public ResearchAgentTools(
            StockDetailService stockDetailService,
            FinnhubClient finnhubClient,
            AlphaVantageClient alphaVantageClient,
            NewsCache newsCache,
            IndicatorCache indicatorCache,
            KnowledgeBaseService knowledgeBaseService,
            @Lazy VectorStore knowledgeBaseVectorStore,
            RetryingToolExecutor retryingToolExecutor) {
        this.stockDetailService = stockDetailService;
        this.finnhubClient = finnhubClient;
        this.alphaVantageClient = alphaVantageClient;
        this.newsCache = newsCache;
        this.indicatorCache = indicatorCache;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeBaseVectorStore = knowledgeBaseVectorStore;
        this.retryingToolExecutor = retryingToolExecutor;
    }

    @Tool(description = "Get the company profile and fundamentals for a stock symbol.")
    public Object getCompanyProfile(@ToolParam(description = "Stock ticker symbol, e.g. AAPL") String symbol) {
        try {
            return retryingToolExecutor.execute(() -> stockDetailService
                    .getStockDetail(symbol, "1day", 5)
                    .<Object>map(profile -> profile)
                    .orElse("No profile data available."));
        } catch (Exception e) {
            return "Error executing getCompanyProfile: " + e.getMessage();
        }
    }

    @Tool(description = "Get recent news headlines and summaries for a stock symbol from the last 7 days.")
    public Object getRecentNews(@ToolParam(description = "Stock ticker symbol, e.g. AAPL") String symbol) {
        try {
            return retryingToolExecutor.execute(() -> {
                List<FinnhubNewsResponse> news =
                        newsCache.get(symbol, List.class).orElse(null);
                if (news == null) {
                    news = finnhubClient.getCompanyNews(symbol, LocalDate.now().minusDays(7), LocalDate.now());
                    newsCache.put(symbol, news);
                }
                String summary = news.stream()
                        .limit(5)
                        .map(n -> "- " + n.headline() + ": " + n.summary())
                        .collect(Collectors.joining("\n"));
                return summary.isEmpty() ? "No recent news." : summary;
            });
        } catch (Exception e) {
            return "Error executing getRecentNews: " + e.getMessage();
        }
    }

    @Tool(description = "Get a technical indicator series for a stock symbol. Indicator must be SMA, EMA, or RSI.")
    public Object getMarketIndicator(
            @ToolParam(description = "Stock ticker symbol, e.g. AAPL") String symbol,
            @ToolParam(description = "Indicator name: SMA, EMA, or RSI") String indicator) {
        try {
            return retryingToolExecutor.execute(() -> {
                String value =
                        indicatorCache.get(symbol, indicator, String.class).orElse(null);
                if (value == null) {
                    value = alphaVantageClient.getIndicator(symbol, indicator);
                    indicatorCache.put(symbol, indicator, value);
                }
                return value.substring(0, Math.min(value.length(), 500));
            });
        } catch (Exception e) {
            return "Error executing getMarketIndicator: " + e.getMessage();
        }
    }

    @Tool(
            description = "Search the RAG knowledge base of ingested news and research notes for context relevant "
                    + "to a question, so the report can be grounded in retrieved sources instead of the model's own "
                    + "memory. Use this when you need historical context, analyst commentary, or background beyond "
                    + "the latest headlines. Results are ranked to favor recent news and include a publishedAt "
                    + "timestamp when known.")
    public Object queryKnowledgeBase(@ToolParam(description = "Natural-language question to search for") String query) {
        try {
            return retryingToolExecutor.execute(() ->
                    (List<KnowledgeChunkResult>) knowledgeBaseService.denseSearch(knowledgeBaseVectorStore, query, 5));
        } catch (Exception e) {
            return "Error executing queryKnowledgeBase: " + e.getMessage();
        }
    }
}
