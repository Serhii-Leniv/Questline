package com.questline.ai;

import java.util.List;

/**
 * Structured plan returned by the LLM (Spring AI maps the model's JSON into this record). It is
 * one of the {@code ai/} package's own types, so the rest of the app never sees Spring AI.
 */
public record GeneratedPlan(String summary, List<PlannedMilestone> milestones) {
}
