package com.questline.web;

import jakarta.validation.constraints.NotBlank;

/** Free-text roadmap to parse into a goal tree (Flow B). */
public record RoadmapInput(@NotBlank String text) {
}
