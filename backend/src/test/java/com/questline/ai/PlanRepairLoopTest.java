package com.questline.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PlanRepairLoopTest {

    private static final GeneratedPlan VALID = new GeneratedPlan("s", List.of(
            new PlannedMilestone("M", "d", List.of(new PlannedTask("T", "d", 10)))));
    private static final GeneratedPlan INVALID = new GeneratedPlan("s", List.of());

    @Test
    void returnsImmediatelyWhenFirstAttemptIsValid() {
        AtomicInteger calls = new AtomicInteger();
        GeneratedPlan result = new PlanRepairLoop(3).run(hint -> {
            calls.incrementAndGet();
            return VALID;
        }, PlanValidator::validate);
        assertThat(result).isSameAs(VALID);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void retriesWithRepairHintThenSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        StringBuilder secondHint = new StringBuilder();
        GeneratedPlan result = new PlanRepairLoop(3).run(hint -> {
            int n = calls.incrementAndGet();
            if (n == 1) {
                return INVALID; // fails validation, feeds a hint into the next attempt
            }
            if (hint != null) {
                secondHint.append(hint);
            }
            return VALID;
        }, PlanValidator::validate);
        assertThat(result).isSameAs(VALID);
        assertThat(calls.get()).isEqualTo(2);
        assertThat(secondHint).isNotEmpty(); // the validation message was passed back
    }

    @Test
    void givesUpAfterMaxAttempts() {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> new PlanRepairLoop(3).run(hint -> {
            calls.incrementAndGet();
            return INVALID;
        }, PlanValidator::validate))
                .isInstanceOf(PlanGenerationException.class)
                .hasMessageContaining("3 attempts");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void firstAttemptReceivesNullHint() {
        new PlanRepairLoop(1).run(hint -> {
            assertThat(hint).isNull();
            return VALID;
        }, PlanValidator::validate);
    }
}
