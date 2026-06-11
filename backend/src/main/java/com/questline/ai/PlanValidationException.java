package com.questline.ai;

/**
 * Thrown when an LLM-produced plan is structurally invalid. Its message is fed back to the model
 * as a repair instruction (see {@link PlanRepairLoop}).
 */
public class PlanValidationException extends RuntimeException {

    public PlanValidationException(String message) {
        super(message);
    }
}
