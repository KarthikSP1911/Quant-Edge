package com.quantedge.backend.rag.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Reranks candidate chunks with the Groq chat model instead of a dedicated cross-encoder - no
 * cross-encoder model is available in this stack without a new heavyweight dependency, so this is
 * an LLM-as-reranker approximation. Labeled as such (not "cross-encoder rerank") in the README's
 * results table, per the eval harness's honesty rule.
 */
@Service
public class LlmRerankService {

    private static final Pattern INDEX_PATTERN = Pattern.compile("\\d+");
    private static final int MAX_CANDIDATE_CHARS = 400;

    private final ChatClient chatClient;

    public LlmRerankService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public List<KnowledgeChunkResult> rerank(String query, List<KnowledgeChunkResult> candidates, int topN) {
        if (candidates.size() <= 1) {
            return candidates;
        }
        String response =
                chatClient.prompt().user(buildPrompt(query, candidates)).call().content();
        List<Integer> order = parseOrder(response, candidates.size());

        List<KnowledgeChunkResult> reranked = new ArrayList<>(candidates.size());
        for (int oneBasedIndex : order) {
            if (oneBasedIndex >= 1 && oneBasedIndex <= candidates.size()) {
                KnowledgeChunkResult candidate = candidates.get(oneBasedIndex - 1);
                if (!reranked.contains(candidate)) {
                    reranked.add(candidate);
                }
            }
        }
        for (KnowledgeChunkResult candidate : candidates) {
            if (!reranked.contains(candidate)) {
                reranked.add(candidate);
            }
        }
        return reranked.subList(0, Math.min(topN, reranked.size()));
    }

    private String buildPrompt(String query, List<KnowledgeChunkResult> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(query).append("\n\n");
        sb.append("Rank the following passages from most to least relevant to answering the question.\n");
        sb.append("Respond with ONLY a comma-separated list of passage numbers, most relevant first, ")
                .append("no explanation. Example: 3,1,4,2\n\n");
        for (int i = 0; i < candidates.size(); i++) {
            String text = candidates.get(i).text();
            if (text.length() > MAX_CANDIDATE_CHARS) {
                text = text.substring(0, MAX_CANDIDATE_CHARS);
            }
            sb.append("Passage ").append(i + 1).append(": ").append(text).append("\n\n");
        }
        return sb.toString();
    }

    private List<Integer> parseOrder(String response, int candidateCount) {
        List<Integer> order = new ArrayList<>();
        if (response == null) {
            return order;
        }
        Matcher matcher = INDEX_PATTERN.matcher(response);
        while (matcher.find() && order.size() < candidateCount) {
            order.add(Integer.parseInt(matcher.group()));
        }
        return order;
    }
}
