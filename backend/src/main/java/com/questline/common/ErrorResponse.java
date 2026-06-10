package com.questline.common;

/**
 * Consistent error payload returned for every handled error: {@code { code, message }}.
 */
public record ErrorResponse(String code, String message) {
}
