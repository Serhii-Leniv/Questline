package com.questline.ai;

/** Thrown when the model could not produce a valid plan within the allowed repair attempts. */
public class PlanGenerationException extends RuntimeException {

    public PlanGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
