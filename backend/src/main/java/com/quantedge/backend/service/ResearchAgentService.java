package com.quantedge.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.quantedge.backend.cache.IndicatorCache;
import com.quantedge.backend.cache.NewsCache;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.ResearchNote;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.AlphaVantageClient;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.dto.FinnhubNewsResponse;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.ResearchNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchAgentService {

    private final CompanyRepository companyRepository;
    private final StockDetailService stockDetailService;
    private final FinnhubClient finnhubClient;
    private final AlphaVantageClient alphaVantageClient;
    private final ChatClient chatClient;
    private final ResearchNoteRepository researchNoteRepository;
    private final SseTraceService sseTraceService;
    private final NewsCache newsCache;
    private final IndicatorCache indicatorCache;

    private final Set<UUID> activeUsers = ConcurrentHashMap.newKeySet();

    public void runResearch(User user, String symbol, String sessionId) {
        if (!activeUsers.add(user.getId())) {
            sseTraceService.initSession(sessionId, user.getId());
            sendTrace(sessionId, "error", "Another research task is already running for this user.");
            sseTraceService.complete(sessionId);
            return;
        }

        sseTraceService.initSession(sessionId, user.getId());
        try {
            Company company = companyRepository
                    .findBySymbol(symbol)
                    .orElseThrow(() -> new CompanyNotFoundException("Symbol not found: " + symbol));

            String stalenessWarning = "";

            // Step 1: Company Profile
            sendTrace(sessionId, "fetching_profile", "Fetching company profile for " + symbol + "...");
            String profileData = "N/A";
            try {
                var profile =
                        stockDetailService.getStockDetail(symbol, "1day", 5).orElse(null);
                profileData = profile != null ? profile.toString() : "N/A";
            } catch (Exception e) {
                log.warn("Failed to fetch profile for {}. Degrading gracefully.", symbol, e);
                stalenessWarning += "Note: Company profile could not be fetched. ";
            }

            // Step 2: Recent News
            sendTrace(sessionId, "fetching_news", "Fetching recent news...");
            String newsSummary = "No recent news.";
            try {
                List<FinnhubNewsResponse> news =
                        newsCache.get(symbol, List.class).orElse(null);
                if (news == null) {
                    news = finnhubClient.getCompanyNews(symbol, LocalDate.now().minusDays(7), LocalDate.now());
                    newsCache.put(symbol, news);
                }

                newsSummary = news.stream()
                        .limit(5)
                        .map(n -> "- " + n.headline() + ": " + n.summary())
                        .collect(Collectors.joining("\n"));
                if (newsSummary.isEmpty()) {
                    newsSummary = "No recent news.";
                }
            } catch (Exception e) {
                log.warn("Failed to fetch news for {}. Degrading gracefully.", symbol, e);
                stalenessWarning += "Note: Recent news could not be fetched. ";
            }

            // Step 3: Market Indicators (with 429 graceful degradation)
            sendTrace(sessionId, "fetching_indicators", "Analyzing market indicators (SMA, EMA, RSI)...");
            String indicatorsData = "N/A";
            try {
                String sma = indicatorCache.get(symbol, "SMA", String.class).orElse(null);
                if (sma == null) {
                    sma = alphaVantageClient.getIndicator(symbol, "SMA");
                    indicatorCache.put(symbol, "SMA", sma);
                }

                String rsi = indicatorCache.get(symbol, "RSI", String.class).orElse(null);
                if (rsi == null) {
                    rsi = alphaVantageClient.getIndicator(symbol, "RSI");
                    indicatorCache.put(symbol, "RSI", rsi);
                }

                indicatorsData = "SMA Data:\n" + sma.substring(0, Math.min(sma.length(), 200)) + "...\n" + "RSI Data:\n"
                        + rsi.substring(0, Math.min(rsi.length(), 200)) + "...";
            } catch (Exception e) {
                log.warn("Failed to fetch indicators for {}. Degrading gracefully.", symbol, e);
                stalenessWarning += "Note: Some recent market indicators could not be fetched due to API limits. ";
            }

            if (profileData.equals("N/A") && newsSummary.equals("No recent news.") && indicatorsData.equals("N/A")) {
                throw new ExternalApiException("All external APIs failed. Cannot generate report.", null);
            }

            // Step 4: LLM Synthesis
            sendTrace(sessionId, "synthesizing_report", "Synthesizing research report via AI...");
            String promptString =
                    """
                    You are an expert financial analyst. Write a comprehensive research report for {symbol}.

                    Company Profile:
                    {profile}

                    Recent News:
                    {news}

                    Market Indicators:
                    {indicators}

                    {stalenessWarning}

                    Format the report in Markdown. Include sections for: Executive Summary, Fundamentals, Recent Developments, Technical Analysis, and Conclusion.
                    """;

            PromptTemplate template = new PromptTemplate(promptString);
            template.add("symbol", symbol);
            template.add("profile", profileData);
            template.add("news", newsSummary);
            template.add("indicators", indicatorsData);
            template.add("stalenessWarning", stalenessWarning);

            String reportContent = chatClient
                    .prompt()
                    .messages(template.createMessage())
                    .call()
                    .content();

            // Step 5: Save Report
            sendTrace(sessionId, "saving_report", "Saving final report to database...");
            ResearchNote note = ResearchNote.builder()
                    .user(user)
                    .company(company)
                    .title("Research Report: " + symbol)
                    .content(reportContent)
                    .generatedBy("AGENT")
                    .build();
            researchNoteRepository.save(note);

            sendTrace(sessionId, "complete", "Research complete!");
            sseTraceService.complete(sessionId);

        } catch (Exception e) {
            log.error("Research agent failed", e);
            sendTrace(sessionId, "error", "Agent failed: " + e.getMessage());
            sseTraceService.complete(sessionId);
        } finally {
            activeUsers.remove(user.getId());
        }
    }

    private void sendTrace(String sessionId, String step, String message) {
        sseTraceService.sendEvent(sessionId, "trace", new TraceEvent(step, message));
    }

    public record TraceEvent(String step, String message) {}
}
