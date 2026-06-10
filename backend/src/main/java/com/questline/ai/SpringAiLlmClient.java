package com.questline.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Spring-AI-backed implementation of {@link LlmClient}. This is the ONLY place in the app that
 * imports Spring AI types.
 *
 * <p>The {@code ChatClient.Builder} is injected via {@link ObjectProvider} so the application
 * boots cleanly even when no provider is configured (e.g. tests, or a missing API key): the
 * client simply reports {@link #isConfigured()} {@code false}.
 */
@Component
public class SpringAiLlmClient implements LlmClient {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;

    public SpringAiLlmClient(ObjectProvider<ChatClient.Builder> chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public boolean isConfigured() {
        return chatClientBuilder.getIfAvailable() != null;
    }

    @Override
    public LlmReply complete(String systemPrompt, String userPrompt) {
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("No LLM provider configured (set GEMINI_API_KEY)");
        }
        String text = builder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        return new LlmReply(text);
    }
}
