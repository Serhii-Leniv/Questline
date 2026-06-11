package com.questline.ai;

/**
 * Questline's LLM seam. Services depend on this interface only — never on Spring AI's
 * {@code ChatClient} directly — so RC→GA churn, or a future swap to LangChain4j / a custom
 * Gemini RestClient, stays contained to the {@code ai/} package.
 *
 * <p>Phase 0 exposes just a configuration probe and a minimal completion call; real planning
 * methods (returning our own {@code GeneratedPlan} records) arrive in Phase 1.
 */
public interface LlmClient {

    /** Whether a provider is wired up (API key present, model bean available). */
    boolean isConfigured();

    /** Minimal single-shot completion. Phase 0 sanity use only. */
    LlmReply complete(String systemPrompt, String userPrompt);

    /**
     * Generates a decomposed roadmap from the user's goal input. The returned plan is already
     * validated (structurally sound) — the implementation runs the validate/repair loop internally.
     *
     * @throws PlanGenerationException if no valid plan could be produced within the repair budget
     * @throws IllegalStateException   if no provider is configured
     */
    GeneratedPlan generatePlan(PlanRequest request);
}
