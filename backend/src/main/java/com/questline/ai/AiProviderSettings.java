package com.questline.ai;

/**
 * A user's own OpenAI-compatible provider credentials (BYOK). When present, the LLM call uses
 * these instead of the server default — so each user can plug in their OpenRouter / OpenAI / Groq
 * / local key. Free of Spring AI types; only {@code SpringAiLlmClient} turns it into a client.
 */
public record AiProviderSettings(String baseUrl, String apiKey, String model) {
}
