package com.questline.ai;

/**
 * Validates the structure of LLM output. AI output is never trusted: every plan/decomposition
 * passes through here before it can be persisted. Failures carry a human-readable reason that
 * doubles as the repair instruction.
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
                validateTask(task, "milestone \"" + milestone.title() + "\"");
            }
        }
    }

    public static void validateSubtasks(Subtasks decomposition) {
        if (decomposition == null || decomposition.subtasks() == null
                || decomposition.subtasks().isEmpty()) {
            throw new PlanValidationException("the decomposition must contain at least one subtask");
        }
        for (PlannedTask task : decomposition.subtasks()) {
            validateTask(task, "the decomposition");
        }
    }

    private static void validateTask(PlannedTask task, String where) {
        if (isBlank(task.title())) {
            throw new PlanValidationException("every task must have a non-empty title (in " + where + ")");
        }
        if (task.estimateMinutes() != null && task.estimateMinutes() <= 0) {
            throw new PlanValidationException("task \"" + task.title()
                    + "\" has a non-positive estimateMinutes; omit it or use a positive value");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
