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

    private static final int MAX_PLAN_ATTEMPTS = 3;

    private static final String PLAN_SYSTEM_PROMPT = """
            You are the planning engine of Questline. Build a realistic, decomposed roadmap
            from the user's current situation to their target by the deadline.

            Rules:
            - Order milestones by dependency (prerequisites first).
            - Each Task must be atomic: doable in one focused sitting; set a realistic
              estimateMinutes (a positive whole number of minutes).
            - Respect the weekly capacity — don't produce more work than fits the timeframe;
              if it doesn't fit, say so in `summary` and prioritize.
            - Skip topics the user already knows.
            - Every milestone must have at least one task.
            - Write all human-readable text (titles/descriptions/summary) in the USER'S language.
            """;

    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;
    private final PlanRepairLoop repairLoop = new PlanRepairLoop(MAX_PLAN_ATTEMPTS);

    public SpringAiLlmClient(ObjectProvider<ChatClient.Builder> chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public boolean isConfigured() {
        return chatClientBuilder.getIfAvailable() != null;
    }

    @Override
    public LlmReply complete(String systemPrompt, String userPrompt) {
        String text = requireClient()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        return new LlmReply(text);
    }

    @Override
    public GeneratedPlan generatePlan(PlanRequest request) {
        ChatClient client = requireClient();
        return repairLoop.run(repairHint -> {
            String userPrompt = buildUserPrompt(request, repairHint);
            try {
                GeneratedPlan plan = client.prompt()
                        .system(PLAN_SYSTEM_PROMPT)
                        .user(userPrompt)
                        .call()
                        .entity(GeneratedPlan.class);
                if (plan == null) {
                    throw new PlanValidationException("the model returned no parseable plan");
                }
                return plan;
            } catch (PlanValidationException e) {
                throw e;
            } catch (RuntimeException e) {
                // Structured-output conversion failed (malformed JSON, wrong shape). Treat it as a
                // repairable content error; genuine transient provider errors are already retried
                // by Spring AI before reaching here.
                throw new PlanValidationException("the output could not be parsed as a plan: " + e.getMessage());
            }
        });
    }

    private ChatClient requireClient() {
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("No LLM provider configured (set GEMINI_API_KEY)");
        }
        return builder.build();
    }

    private static String buildUserPrompt(PlanRequest request, String repairHint) {
        StringBuilder sb = new StringBuilder()
                .append("Current situation (context): ").append(request.context()).append('\n')
                .append("Target: ").append(request.target()).append('\n');
        if (request.targetDate() != null) {
            sb.append("Deadline: ").append(request.targetDate()).append('\n');
        }
        if (request.weeklyCapacityMinutes() != null) {
            sb.append("Weekly capacity (minutes): ").append(request.weeklyCapacityMinutes()).append('\n');
        }
        if (repairHint != null) {
            sb.append("\nYour previous answer was rejected: ").append(repairHint)
                    .append("\nReturn a corrected plan that fixes exactly this problem.");
        }
        return sb.toString();
    }
}
