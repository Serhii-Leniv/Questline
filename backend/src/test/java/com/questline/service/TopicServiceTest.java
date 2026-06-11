package com.questline.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TopicServiceTest {

    @Test
    void slugifyHandlesSpacesPunctuationAndUnicode() {
        assertThat(TopicService.slugify("System Design")).isEqualTo("system-design");
        assertThat(TopicService.slugify("  Spring   Boot!! ")).isEqualTo("spring-boot");
        assertThat(TopicService.slugify("C++")).isEqualTo("c");
        assertThat(TopicService.slugify("Бази даних")).isEqualTo("бази-даних"); // Unicode preserved
    }
}
