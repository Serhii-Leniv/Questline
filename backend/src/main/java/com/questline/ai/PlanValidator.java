package com.questline.ai;

/**
 * Validates the structure of an LLM-produced {@link GeneratedPlan}. AI output is never trusted:
 * every plan passes through here before it can be persisted. Failures carry a human-readable
 * reason that doubles as the repair instruction.
 */
public final class PlanValidator {

    private PlanValidator() {
    }

    public static void validate(GeneratedPlan plan) {
        if (plan == null) {
            throw new PlanValidationException("the plan was empty");
        }
        if (plan.milestones() == null || plan.milestones().isEmpty()) {
            throw new PlanValidationException("the plan must contain at least one milestone");
        }
        for (PlannedMilestone milestone : plan.milestones()) {
            if (isBlank(milestone.title())) {
                throw new PlanValidationException("every milestone must have a non-empty title");
            }
            if (milestone.tasks() == null || milestone.tasks().isEmpty()) {
                throw new PlanValidationException(
                        "milestone \"" + milestone.title() + "\" must contain at least one task");
            }
            for (PlannedTask task : milestone.tasks()) {
                if (isBlank(task.title())) {
                    throw new PlanValidationException("every task must have a non-empty title (in milestone \""
                            + milestone.title() + "\")");
                }
                if (task.estimateMinutes() != null && task.estimateMinutes() <= 0) {
                    throw new PlanValidationException("task \"" + task.title()
                            + "\" has a non-positive estimateMinutes; omit it or use a positive value");
                }
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
