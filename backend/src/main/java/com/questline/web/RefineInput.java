package com.questline.web;

import jakarta.validation.constraints.NotBlank;

/** A chat message refining a generated plan before it is accepted (Flow A). */
public record RefineInput(@NotBlank String message) {
}
