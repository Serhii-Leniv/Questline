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
    private final String model;

    public AiStartupCheck(LlmClient llmClient,
                          @Value("${spring.ai.google.genai.chat.options.model:unset}") String model) {
        this.llmClient = llmClient;
        this.model = model;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (llmClient.isConfigured()) {
            log.info("AI provider configured (model={}). No AI features active in Phase 0.", model);
        } else {
            log.warn("AI provider NOT configured (set GEMINI_API_KEY to enable). "
                    + "This is expected in Phase 0 / tests.");
        }
    }
}
