package com.questline.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenAiCompatibleClientTest {

    @Test
    void extractJsonTakesTheOutermostObject_evenWithFencesOrProse() {
        assertThat(OpenAiCompatibleClient.extractJson("{\"a\":1}")).isEqualTo("{\"a\":1}");
        assertThat(OpenAiCompatibleClient.extractJson("```json\n{\"a\":1}\n```")).isEqualTo("{\"a\":1}");
        assertThat(OpenAiCompatibleClient.extractJson("Here you go: {\"a\":1}. Done."))
                .isEqualTo("{\"a\":1}");
    }

    @Test
    void extractJsonReturnsInputWhenNoBraces() {
        assertThat(OpenAiCompatibleClient.extractJson("nope")).isEqualTo("nope");
    }
}
