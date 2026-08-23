package com.quantedge.backend.config;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

/**
 * Strips Groq reasoning-model "reasoning_content" metadata (see {@link ReasoningContentSupport})
 * from every assistant message right before a {@code ChatClient} request reaches the model, so it
 * never gets re-sent on a later turn in the tool-calling loop.
 */
public class ReasoningContentStrippingAdvisor implements CallAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Prompt prompt = request.prompt();
        Prompt stripped = new Prompt(ReasoningContentSupport.strip(prompt.getInstructions()), prompt.getOptions());
        return chain.nextCall(request.mutate().prompt(stripped).build());
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
