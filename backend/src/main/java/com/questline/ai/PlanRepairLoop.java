package com.questline.ai;

/**
 * Drives the AI repair loop: attempt → validate → on failure, re-prompt the model with the
 * validation error and try again, up to a fixed number of attempts. Provider-side transient
 * errors are handled separately by Spring AI's own retry; this loop is purely about producing a
 * structurally valid plan.
 *
 * <p>The model call is injected as {@link Attempt} so the loop is unit-testable without any LLM.
 */
public class PlanRepairLoop {

    /** A single model attempt. {@code repairHint} is null on the first try, otherwise the prior error. */
    @FunctionalInterface
    public interface Attempt {
        GeneratedPlan call(String repairHint);
    }

    private final int maxAttempts;

    public PlanRepairLoop(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
    }

    public GeneratedPlan run(Attempt attempt) {
        PlanValidationException last = null;
        String hint = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                GeneratedPlan plan = attempt.call(hint);
                PlanValidator.validate(plan);
                return plan;
            } catch (PlanValidationException e) {
                last = e;
                hint = e.getMessage();
            }
        }
        throw new PlanGenerationException(
                "Could not produce a valid plan after " + maxAttempts + " attempts", last);
    }
}
