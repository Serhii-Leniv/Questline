package com.questline.web;

import jakarta.validation.constraints.NotBlank;

/**
 * BYOK settings: an OpenAI-compatible endpoint + model + API key. A blank/omitted {@code apiKey}
 * keeps the previously stored key (so the user can edit base-url/model without resending it).
 */
public record AiSettingsRequest(
        @NotBlank String baseUrl,
        @NotBlank String model,
        String apiKey
) {
}
