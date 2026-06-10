package com.questline.domain;

/**
 * A single learning/reference link attached to a {@link Task}.
 * Stored inside the {@code tasks.resources} JSON column.
 */
public record ResourceLink(String title, String url) {
}
