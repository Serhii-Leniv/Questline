package com.questline.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Logs the AI provider configuration state at startup. Intentionally makes NO network call —
 * Phase 0 builds no AI features; this is only a wiring sanity check.
 */
@Component
public class AiStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiStartupCheck.class);

    private final LlmClient llmClient;
    private final String provider;
    private final String geminiModel;
    private final String openaiModel;

    public AiStartupCheck(LlmClient llmClient,
                          @Value("${spring.ai.model.chat:unset}") String provider,
                          @Value("${spring.ai.google.genai.chat.options.model:unset}") String geminiModel,
                          @Value("${spring.ai.openai.chat.options.model:unset}") String openaiModel) {
        this.llmClient = llmClient;
        this.provider = provider;
        this.geminiModel = geminiModel;
        this.openaiModel = openaiModel;
    }

    @Override
    public void run(ApplicationArguments args) {
        String model = "openai".equals(provider) ? openaiModel : geminiModel;
        if (llmClient.isConfigured()) {
            log.info("AI provider configured: {} (model={}).", provider, model);
        } else {
            log.warn("AI provider NOT configured (provider={}). Set the provider's API key to enable "
                    + "(GEMINI_API_KEY for google-genai, OPENAI_API_KEY for openai). Expected in tests.",
                    provider);
        }
    }
}
