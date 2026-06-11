package com.questline.ai;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Calls a user's own OpenAI-compatible endpoint (BYOK) directly over HTTP — OpenRouter, OpenAI,
 * Groq, or a local server (Ollama, LM Studio). Deliberately free of Spring AI: it speaks the
 * standard {@code POST /chat/completions} contract, asks for a JSON object, then parses + validates
 * + repairs using the same {@link PlanRepairLoop} as the default path.
 */
@Component
public class OpenAiCompatibleClient {

    private static final int MAX_ATTEMPTS = 3;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final PlanRepairLoop repairLoop = new PlanRepairLoop(MAX_ATTEMPTS);

    public OpenAiCompatibleClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    public <T> T call(AiProviderSettings settings, Class<T> type, String systemPrompt,
                      UnaryOperator<String> userPromptForHint, String jsonShape, String noun,
                      PlanRepairLoop.Validator<T> validator) {
        RestClient client = restClientBuilder.baseUrl(settings.baseUrl()).build();
        return repairLoop.run(repairHint -> {
            String userPrompt = userPromptForHint.apply(repairHint) + "\n\n" + jsonShape
                    + "\nReturn ONLY that JSON object — no prose, no markdown fences.";
            try {
                String content = requestContent(client, settings, systemPrompt, userPrompt);
                T value = objectMapper.readValue(extractJson(content), type);
                if (value == null) {
                    throw new PlanValidationException("the model returned no parseable " + noun);
                }
                return value;
            } catch (PlanValidationException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new PlanValidationException(
                        "the output could not be parsed as a " + noun + ": " + e.getMessage());
            }
        }, validator);
    }

    @SuppressWarnings("unchecked")
    private String requestContent(RestClient client, AiProviderSettings settings,
                                  String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", settings.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "response_format", Map.of("type", "json_object"));

        Map<String, Object> response = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + settings.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new PlanValidationException("empty response from the provider");
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new PlanValidationException("the provider returned no choices");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        Object content = message == null ? null : message.get("content");
        if (content == null) {
            throw new PlanValidationException("the provider returned no message content");
        }
        return content.toString();
    }

    /** Tolerates markdown fences / surrounding prose by taking the outermost JSON object. */
    static String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        return (start >= 0 && end > start) ? content.substring(start, end + 1) : content;
    }
}
