package com.questline.ai;

import java.util.function.UnaryOperator;
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
            - Tag each task with 1–3 concise `topics` (short subject labels).
            - Write all human-readable text (titles/descriptions/summary) in the USER'S language.
            """;

    private static final String PARSE_SYSTEM_PROMPT = """
            You parse a user-provided roadmap into Questline's structured plan. Extract the
            milestones and their tasks faithfully from the text — do NOT invent content beyond
            what the text states or clearly implies.

            Rules:
            - Preserve the order given in the text.
            - Every milestone must have at least one task.
            - Set estimateMinutes only if the text suggests a duration; otherwise leave it null.
            - Keep all human-readable text in the text's original language.
            """;

    private static final String DECOMPOSE_SYSTEM_PROMPT = """
            You break a single task into smaller, concrete subtasks for Questline. Use the goal
            context to stay relevant.

            Rules:
            - Each subtask must be atomic: doable in one focused sitting, with a realistic
              estimateMinutes (a positive whole number) when you can judge it.
            - Produce 2–6 subtasks; don't pad.
            - Keep all human-readable text in the user's language.
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
        return runStructured(PLAN_SYSTEM_PROMPT, hint -> buildUserPrompt(request, hint));
    }

    @Override
    public GeneratedPlan parseRoadmap(String roadmapText) {
        return runStructured(PARSE_SYSTEM_PROMPT, hint -> buildParsePrompt(roadmapText, hint));
    }

    @Override
    public java.util.List<PlannedTask> decomposeTask(String taskContext) {
        Subtasks result = runEntity(Subtasks.class, DECOMPOSE_SYSTEM_PROMPT,
                hint -> buildDecomposePrompt(taskContext, hint),
                "subtasks", PlanValidator::validateSubtasks);
        return result.subtasks();
    }

    /** Runs one plan-shaped structured-output call through the validate/repair loop. */
    private GeneratedPlan runStructured(String systemPrompt, UnaryOperator<String> userPromptForHint) {
        return runEntity(GeneratedPlan.class, systemPrompt, userPromptForHint, "plan",
                PlanValidator::validate);
    }

    /** Generic: prompt → map to {@code type} → validate, with a repair loop on invalid output. */
    private <T> T runEntity(Class<T> type, String systemPrompt,
                            UnaryOperator<String> userPromptForHint, String noun,
                            PlanRepairLoop.Validator<T> validator) {
        ChatClient client = requireClient();
        return repairLoop.run(repairHint -> {
            try {
                T value = client.prompt()
                        .system(systemPrompt)
                        .user(userPromptForHint.apply(repairHint))
                        .call()
                        .entity(type);
                if (value == null) {
                    throw new PlanValidationException("the model returned no parseable " + noun);
                }
                return value;
            } catch (PlanValidationException e) {
                throw e;
            } catch (RuntimeException e) {
                // Structured-output conversion failed (malformed JSON, wrong shape). Treat it as a
                // repairable content error; genuine transient provider errors are already retried
                // by Spring AI before reaching here.
                throw new PlanValidationException(
                        "the output could not be parsed as a " + noun + ": " + e.getMessage());
            }
        }, validator);
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

    private static String buildParsePrompt(String roadmapText, String repairHint) {
        StringBuilder sb = new StringBuilder("Parse this roadmap into the structured plan:\n\n")
                .append(roadmapText);
        if (repairHint != null) {
            sb.append("\n\nYour previous answer was rejected: ").append(repairHint)
                    .append("\nReturn a corrected plan that fixes exactly this problem.");
        }
        return sb.toString();
    }

    private static String buildDecomposePrompt(String taskContext, String repairHint) {
        StringBuilder sb = new StringBuilder(taskContext);
        if (repairHint != null) {
            sb.append("\n\nYour previous answer was rejected: ").append(repairHint)
                    .append("\nReturn corrected subtasks that fix exactly this problem.");
        }
        return sb.toString();
    }
}
