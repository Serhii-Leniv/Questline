package com.questline.ai;

/**
 * Our own reply type. Deliberately free of any Spring AI / provider types so the rest of the
 * app never depends on the LLM SDK.
 */
public record LlmReply(String text) {
}
