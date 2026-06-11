package com.questline.ai;

/**
 * Drives the AI repair loop: attempt → validate → on failure, re-prompt the model with the
 * validation error and try again, up to a fixed number of attempts. Provider-side transient
 * errors are handled separately by Spring AI's own retry; this loop is purely about producing a
 * structurally valid result.
 *
 * <p>Generic over the result type so it serves plan generation, roadmap parsing, and task
 * decomposition. The model call ({@link Attempt}) and the check ({@link Validator}) are injected,
 * so the loop is unit-testable without any LLM.
 */
public class PlanRepairLoop {

    /** A single model attempt. {@code repairHint} is null on the first try, otherwise the prior error. */
    @FunctionalInterface
    public interface Attempt<T> {
        T call(String repairHint);
    }

    /** Validates a result, throwing {@link PlanValidationException} (whose message becomes the hint). */
    @FunctionalInterface
    public interface Validator<T> {
        void validate(T value);
    }

    private final int maxAttempts;

    public PlanRepairLoop(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
    }

    public <T> T run(Attempt<T> attempt, Validator<T> validator) {
        PlanValidationException last = null;
        String hint = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                T value = attempt.call(hint);
                validator.validate(value);
                return value;
            } catch (PlanValidationException e) {
                last = e;
                hint = e.getMessage();
            }
        }
        throw new PlanGenerationException(
                "Could not produce a valid result after " + maxAttempts + " attempts", last);
    }
}
