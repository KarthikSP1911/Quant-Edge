package com.quantedge.backend.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

/**
 * Groq's reasoning models (e.g. openai/gpt-oss-120b) return a "reasoning_content" field on
 * assistant messages. Spring AI's OpenAiChatModel stashes it into {@link AssistantMessage}
 * metadata under "reasoningContent" and echoes it straight back as an outgoing
 * "reasoning_content" property if that same message is replayed on a later request - which Groq's
 * API rejects with a 400 since that property is output-only. Strip it before any assistant
 * message (whether returned by the model or hand-built, e.g. a synthesized plan turn) goes back
 * out on a subsequent call.
 *
 * <p>Shared by {@link ReasoningContentStrippingAdvisor} (for {@code ChatClient}-based calls, which
 * run this automatically via the advisor chain) and any caller driving {@code ChatModel} directly
 * (which must call {@link #strip(List)} itself before each {@code chatModel.call(...)}).
 */
public final class ReasoningContentSupport {

    private ReasoningContentSupport() {}

    public static List<Message> strip(List<Message> messages) {
        return messages.stream().map(ReasoningContentSupport::stripIfNeeded).toList();
    }

    private static Message stripIfNeeded(Message message) {
        if (!(message instanceof AssistantMessage assistantMessage)
                || !assistantMessage.getMetadata().containsKey("reasoningContent")) {
            return message;
        }
        Map<String, Object> metadata = new HashMap<>(assistantMessage.getMetadata());
        metadata.remove("reasoningContent");
        return AssistantMessage.builder()
                .content(assistantMessage.getText())
                .properties(metadata)
                .toolCalls(assistantMessage.getToolCalls())
                .media(assistantMessage.getMedia())
                .build();
    }
}
