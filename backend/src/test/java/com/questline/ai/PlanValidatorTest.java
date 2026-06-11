package com.questline.ai;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlanValidatorTest {

    @Test
    void acceptsAWellFormedPlan() {
        GeneratedPlan plan = new GeneratedPlan("summary", List.of(
                new PlannedMilestone("Basics", "desc", List.of(
                        new PlannedTask("Read intro", "desc", 30)))));
        assertThatCode(() -> PlanValidator.validate(plan)).doesNotThrowAnyException();
    }

    @Test
    void rejectsNoMilestones() {
        assertThatThrownBy(() -> PlanValidator.validate(new GeneratedPlan("s", List.of())))
                .isInstanceOf(PlanValidationException.class)
                .hasMessageContaining("at least one milestone");
    }

    @Test
    void rejectsBlankMilestoneTitle() {
        GeneratedPlan plan = new GeneratedPlan("s", List.of(
                new PlannedMilestone(" ", "d", List.of(new PlannedTask("T", "d", 10)))));
        assertThatThrownBy(() -> PlanValidator.validate(plan))
                .isInstanceOf(PlanValidationException.class)
                .hasMessageContaining("title");
    }

    @Test
    void rejectsMilestoneWithNoTasks() {
        GeneratedPlan plan = new GeneratedPlan("s", List.of(
                new PlannedMilestone("M", "d", List.of())));
        assertThatThrownBy(() -> PlanValidator.validate(plan))
                .isInstanceOf(PlanValidationException.class)
                .hasMessageContaining("at least one task");
    }

    @Test
    void rejectsBlankTaskTitle() {
        GeneratedPlan plan = new GeneratedPlan("s", List.of(
                new PlannedMilestone("M", "d", List.of(new PlannedTask("", "d", 10)))));
        assertThatThrownBy(() -> PlanValidator.validate(plan))
                .isInstanceOf(PlanValidationException.class)
                .hasMessageContaining("task");
    }

    @Test
    void rejectsNonPositiveEstimate() {
        GeneratedPlan plan = new GeneratedPlan("s", List.of(
                new PlannedMilestone("M", "d", List.of(new PlannedTask("T", "d", 0)))));
        assertThatThrownBy(() -> PlanValidator.validate(plan))
                .isInstanceOf(PlanValidationException.class)
                .hasMessageContaining("estimateMinutes");
    }

    @Test
    void allowsNullEstimate() {
        GeneratedPlan plan = new GeneratedPlan("s", List.of(
                new PlannedMilestone("M", "d", List.of(new PlannedTask("T", "d", null)))));
        assertThatCode(() -> PlanValidator.validate(plan)).doesNotThrowAnyException();
    }

    @Test
    void acceptsValidSubtasks() {
        Subtasks subtasks = new Subtasks(List.of(
                new PlannedTask("Step 1", "d", 20), new PlannedTask("Step 2", null, null)));
        assertThatCode(() -> PlanValidator.validateSubtasks(subtasks)).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptySubtasks() {
        assertThatThrownBy(() -> PlanValidator.validateSubtasks(new Subtasks(List.of())))
                .isInstanceOf(PlanValidationException.class)
                .hasMessageContaining("at least one subtask");
    }

    @Test
    void rejectsBlankSubtaskTitle() {
        Subtasks subtasks = new Subtasks(List.of(new PlannedTask(" ", "d", 10)));
        assertThatThrownBy(() -> PlanValidator.validateSubtasks(subtasks))
                .isInstanceOf(PlanValidationException.class)
                .hasMessageContaining("title");
    }
}
