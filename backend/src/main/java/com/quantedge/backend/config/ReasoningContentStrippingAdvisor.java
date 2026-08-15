package com.quantedge.backend.config;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

/**
 * Groq's reasoning models (e.g. openai/gpt-oss-120b) return a "reasoning_content" field on
 * assistant messages. Spring AI's OpenAiChatModel stashes it into AssistantMessage metadata under
 * "reasoningContent" and echoes it straight back as an outgoing "reasoning_content" property on
 * the next request in the tool-calling loop - which Groq's API rejects with a 400 since that
 * property is output-only. Strip it here, right before the request reaches the model, so it never
 * gets re-sent.
 */
public class ReasoningContentStrippingAdvisor implements CallAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(stripReasoningContent(request));
    }

    private ChatClientRequest stripReasoningContent(ChatClientRequest request) {
        Prompt prompt = request.prompt();
        List<Message> messages =
                prompt.getInstructions().stream().map(this::stripIfNeeded).toList();
        return request.mutate()
                .prompt(new Prompt(messages, prompt.getOptions()))
                .build();
    }

    private Message stripIfNeeded(Message message) {
        if (!(message instanceof AssistantMessage assistantMessage)
                || !assistantMessage.getMetadata().containsKey("reasoningContent")) {
            return message;
        }
        Map<String, Object> metadata = new java.util.HashMap<>(assistantMessage.getMetadata());
        metadata.remove("reasoningContent");
        return AssistantMessage.builder()
                .content(assistantMessage.getText())
                .properties(metadata)
                .toolCalls(assistantMessage.getToolCalls())
                .media(assistantMessage.getMedia())
                .build();
    }

    @Override
    public String getName() {
        return "ReasoningContentStrippingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
