package com.quantedge.backend.service;

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
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
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

    public void runResearch(User user, String symbol, String sessionId) {
        try {
            Company company = companyRepository
                    .findBySymbol(symbol)
                    .orElseThrow(() -> new CompanyNotFoundException("Symbol not found: " + symbol));

            // Step 1: Company Profile
            sendTrace(sessionId, "fetching_profile", "Fetching company profile for " + symbol + "...");
            var profile = stockDetailService.getStockDetail(symbol, "1day", 5).orElse(null);

            // Step 2: Recent News
            sendTrace(sessionId, "fetching_news", "Fetching recent news...");
            List<FinnhubNewsResponse> news =
                    finnhubClient.getCompanyNews(symbol, LocalDate.now().minusDays(7), LocalDate.now());
            String newsSummary = news.stream()
                    .limit(5)
                    .map(n -> "- " + n.headline() + ": " + n.summary())
                    .collect(Collectors.joining("\n"));

            // Step 3: Market Indicators (with 429 graceful degradation)
            sendTrace(sessionId, "fetching_indicators", "Analyzing market indicators (SMA, EMA, RSI)...");
            String indicatorsData = "";
            String stalenessWarning = "";
            try {
                String sma = alphaVantageClient.getIndicator(symbol, "SMA");
                String rsi = alphaVantageClient.getIndicator(symbol, "RSI");
                indicatorsData = "SMA Data:\n" + sma.substring(0, Math.min(sma.length(), 200)) + "...\n" + "RSI Data:\n"
                        + rsi.substring(0, Math.min(rsi.length(), 200)) + "...";
            } catch (ExternalApiException e) {
                log.warn("Rate limited by Alpha Vantage, degrading gracefully.", e);
                stalenessWarning =
                        "Note: Some recent market indicators could not be fetched due to API rate limits. This report relies on cached or partial data.";
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
            template.add("profile", profile != null ? profile.toString() : "N/A");
            template.add("news", newsSummary.isEmpty() ? "No recent news." : newsSummary);
            template.add("indicators", indicatorsData.isEmpty() ? "N/A" : indicatorsData);
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
        }
    }

    private void sendTrace(String sessionId, String step, String message) {
        sseTraceService.sendEvent(sessionId, "trace", new TraceEvent(step, message));
    }

    public record TraceEvent(String step, String message) {}
}
