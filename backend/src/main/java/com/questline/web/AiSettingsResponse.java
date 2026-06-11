package com.questline.web;

import com.questline.service.AiSettingsService.Status;

/** Non-secret view of a user's AI provider settings — the API key is never returned. */
public record AiSettingsResponse(boolean configured, String baseUrl, String model) {

    public static AiSettingsResponse from(Status status) {
        return new AiSettingsResponse(status.configured(), status.baseUrl(), status.model());
    }
}
